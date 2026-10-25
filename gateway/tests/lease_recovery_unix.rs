#![cfg(all(unix, feature = "test-fixtures"))]
//! An unclean Gateway exit leaves a durable lease behind. On Unix the lease now carries the
//! process group that owned OMP, so the next Gateway can tell "the tree is gone" (reclaim) from
//! "something of that tree still runs, or we have no proof at all" (stay fail-closed).
mod support;

use serde_json::{Value, json};
use std::{
    os::unix::process::CommandExt,
    path::Path,
    process::{Child, Command},
};
use support::{Harness, create_session, runtime_generation, wait_operation};

/// A process group that exists only long enough to be observed and then disappears.
fn vanished_group() -> i32 {
    let child = Command::new("sh")
        .args(["-c", "exit 0"])
        .process_group(0)
        .spawn()
        .unwrap();
    let pgid = child.id() as i32;
    let mut child = child;
    child.wait().unwrap();
    pgid
}

/// A group kept alive by one sleeping member for the duration of a test.
struct LiveGroup(Child);
impl LiveGroup {
    fn start() -> (Self, i32) {
        let child = Command::new("sh")
            .args(["-c", "sleep 60"])
            .process_group(0)
            .spawn()
            .unwrap();
        let pgid = child.id() as i32;
        (Self(child), pgid)
    }
}
impl Drop for LiveGroup {
    fn drop(&mut self) {
        let _ = self.0.kill();
        let _ = self.0.wait();
    }
}

async fn prompt(
    client: &reqwest::Client,
    session: &str,
    credential: &str,
    command_id: &str,
) -> Value {
    assert_eq!(
        client
            .post(format!("{session}/commands"))
            .bearer_auth(credential)
            .json(&json!({"commandId":command_id,"type":"prompt","message":"wake this session"}))
            .send()
            .await
            .unwrap()
            .status(),
        202
    );
    wait_operation(
        client,
        &format!("{session}/operations/{command_id}"),
        credential,
        "failed",
    )
    .await
}

#[tokio::test]
async fn a_lease_whose_process_group_is_gone_is_reclaimed() {
    let harness = Harness::new(1, vec!["--spawn-child-immediate".into()]).await;
    let client = reqwest::Client::new();
    let credential = harness.pair().await;
    let cwd = harness.cwd("reclaim-gone");
    let session = create_session(&client, &harness, &credential, &cwd).await;
    let id = session.rsplit('/').next().unwrap().to_owned();

    assert!(harness.store.reserve_runtime(&id, "crashed-run").unwrap());
    assert!(
        harness
            .store
            .set_runtime_group(&id, "crashed-run", vanished_group())
            .unwrap()
    );
    // The clean-exit path never clears the lease; startup must prove the group is empty first.
    assert_eq!(
        harness.store.runtime_leases().unwrap(),
        vec![(id.clone(), "crashed-run".into())]
    );

    support::command_succeeds(
        &client,
        &session,
        &credential,
        "after-crash",
        json!({"type":"prompt","message":"wake this session"}),
    )
    .await;

    let generation = runtime_generation(&client, &session, &credential).await;
    assert_ne!(generation, "crashed-run");
    assert_eq!(
        harness.store.runtime_leases().unwrap(),
        vec![(id.clone(), generation.clone())],
        "the reclaimed lease must be replaced by the live generation"
    );
    // A started runtime must persist its own group; otherwise its next crash is unrecoverable.
    let (recorded_generation, pgid) = harness
        .store
        .runtime_group(&id)
        .unwrap()
        .expect("a started runtime must record its process group");
    assert_eq!(recorded_generation, generation);
    assert!(pgid > 0);
    assert!(
        pinkcollab_gateway::omp::unix_group_has_live_members(pgid).unwrap(),
        "the recorded group must be the live OMP tree"
    );
}

#[tokio::test]
async fn a_lease_with_a_live_process_group_stays_blocked() {
    let harness = Harness::new(1, vec!["--spawn-child-immediate".into()]).await;
    let client = reqwest::Client::new();
    let credential = harness.pair().await;
    let cwd = harness.cwd("reclaim-live");
    let session = create_session(&client, &harness, &credential, &cwd).await;
    let id = session.rsplit('/').next().unwrap().to_owned();
    let (group, pgid) = LiveGroup::start();

    assert!(harness.store.reserve_runtime(&id, "live-run").unwrap());
    assert!(
        harness
            .store
            .set_runtime_group(&id, "live-run", pgid)
            .unwrap()
    );

    let failed = prompt(&client, &session, &credential, "blocked-live").await;
    assert_eq!(failed["error"]["code"], "runtime_start_failed");
    assert_eq!(
        harness.store.runtime_leases().unwrap(),
        vec![(id, "live-run".into())],
        "a live member is proof the old tree may still write"
    );
    assert!(
        !Path::new(&cwd).join("child.pid").exists(),
        "a blocked lease must not spawn an OMP child"
    );
    drop(group);
}

#[tokio::test]
async fn a_lease_without_a_recorded_group_stays_blocked() {
    let harness = Harness::new(1, vec!["--spawn-child-immediate".into()]).await;
    let client = reqwest::Client::new();
    let credential = harness.pair().await;
    let cwd = harness.cwd("reclaim-legacy");
    let session = create_session(&client, &harness, &credential, &cwd).await;
    let id = session.rsplit('/').next().unwrap().to_owned();

    // Legacy leases predate the pgid column: they carry no proof at all.
    assert!(harness.store.reserve_runtime(&id, "legacy-run").unwrap());
    assert_eq!(harness.store.runtime_group(&id).unwrap(), None);

    let failed = prompt(&client, &session, &credential, "blocked-legacy").await;
    assert_eq!(failed["error"]["code"], "runtime_start_failed");
    assert_eq!(
        harness.store.runtime_leases().unwrap(),
        vec![(id, "legacy-run".into())]
    );
    assert!(!Path::new(&cwd).join("child.pid").exists());
}
