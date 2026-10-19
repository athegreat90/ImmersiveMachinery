# Private NeoForge 26.3 port checkpoint

## Source and scope

- Fork: athegreat90/ImmersiveMachinery; upstream Luke100000/ImmersiveMachinery.
- Branch `26.3-local`; original `work` branch preserved. The user subsequently authorized publishing this branch to the athegreat90 fork.
- Base upstream `26.2`: `5e1f685040a9aaf1551d9504d5b3bfa21f18c365` (0.2.3+26.2).
- Initially clean; development and validation were local. Only branch publication to the user's fork is now authorized; no artifact releases or production changes. The inherited CI excludes `26.3-local` from push triggers to prevent automatic Maven publication.
- Target Minecraft26.3, Java25, NeoForge26.3.0.94, NeoForm26.3-1; mod0.2.3+26.3-local. Original Gradle9.5.1 and ModDevGradle2.0.148 retained.
- Machinery migration began only after Aircraft's actual NeoForge compile, full build and dedicated-server startup passed.

## Completed migration

1. Configure common and NeoForge only; Fabric source untouched and unused plugin resolution removed. Keep shared-source architecture.
2. Composite includeBuild ../ImmersiveAircraft substitutes net.conczin:immersive_aircraft with its actual :neoforge project. Common compileOnly now requests +neoforge; no mislabeled Fabric binary or old remote Aircraft API. Required runtime version exactly [1.7.0+26.3-local]. Actual dependencyInsight confirms project substitution/apiElements/JVM25; evidence machinery-aircraft-resolution.log.
3. SDL H action binding uses actual InputConstants.KEY_H/KEYBOARD; same key registration identifier.
4. BambooBeeScreen uses actual Blaze3D.openUri(URI), replacing removed Util.OS API. Existing water-mask/geometry callback and getWaterVision target signatures were confirmed; no speculative renderer rewrite. No custom shaders found.
5. Network safety: TunnelDigger controls require live connected actual direct pilot and offsets -1..1. Server drilling also requires engine power, matching the existing client restriction. Existing block protection/permission hooks remain.
6. Bee configuration requires the exact live entity's server-owned VehicleScreenHandler and existing distance/inventory/life validity before applying or broadcasting. Client sends dirty update before queuing menu close. Reuse Aircraft's pre-existing getVehicle getter, no new API or persistent IDs.
7. All registry/entity/item/component/recipe/persistence identifiers, licenses and attribution preserved. Common resources are byte-for-byte unchanged from upstream; only NeoForge pack/metadata changed.

## Validation

- Current full main build and separate diagnostic build PASS (9s): `GRADLE_USER_HOME=/workspace/.cache/gradle bash ./gradlew --no-daemon --max-workers=3 --console=plain :neoforge:build :neoforge:portTestJar`. Java25. Log `/workspace/.local-port/machinery-build-final.log`. Main test task NO-SOURCE, not a JUnit pass claim.
- Runtime: neoforge/build/libs/immersive_machinery-0.2.3+26.3-local.jar. Correct metadata/license, no duplicated Aircraft classes, no diagnostic/JUnit classes.
- Real-level server regressions all PASS: vanilla/Aircraft coal1600 and stone0, valid pilot/extreme offset rejection, Bee valid menu/no-menu/distant-menu checks. Report `/workspace/.local-port/server-regressions.json`.
- Diagnostic code isolated in neoforge/src/portTest plus portTestJar. Not part of main JAR. ConnectedTestPlayer is a normal ServerPlayer with NeoForge's public fake-player connection sink; NeoForge FakePlayer itself deliberately refuses riding, explaining an initial fixture-only failure. No real account/network player is used in these direct-handler regressions. These are not a substitute for multiplayer behavior tests.
- Actual official-installer dedicated server loaded both production JARs without the helper, reached Done, spawned all four machines and all seven aircraft. Save/close/reload retained nine Fuel0 values, persistent tags for Gyrodyne/Bee and biplane/digger inventory stacks (7 coal each). Scope: smoke test only.
- Latest icon metadata correction is in Aircraft, whose rebuilt artifact is running on the dedicated test server. Client offscreen Mesa OpenGL4.5 loads both actual JARs, joins the disposable dedicated server and captures real rendered vehicles. Diagnostic screenshot-only helper remains excluded from both main JARs. Normal input/automation/two-player behavior remain UNTESTED.
- No supplied26.2 world: old-world data compatibility UNTESTED. Full mining/automation/water/flight controls, protected-area integrations, multi-user sessions and all upgrades require the manual checklist in local-26.3-jars/README.md.

## References, state and next exact action

Official primer: https://github.com/neoforged/.github/blob/main/primers/26.3/index.md; actual Gradle26.3 patched sources and official ModDevGradle docs. Verified InputConstants, Blaze3D.openUri, SubmitNodeCollection.waterMask/custom geometry and LocalPlayer.getWaterVision, VehicleScreenHandler.stillValid and server handlers.

No pending production compile errors. A temporary diagnostic-only log of a removed Minecraft.screen field failed compilation and was removed; actual framebuffer capture uses verified gameRenderer.mainRenderTarget().

Final client log: `/workspace/.local-port/client-joined-console.log`. Next: execute the remaining manual gameplay/two-player/26.2-world checklist on disposable copies; delivery RESULTADOS.md records the final scope and process shutdown. The subsequent user request authorizes publishing this branch to the user's fork; production changes and artifact releases remain outside the scope.

## Modified tracked files

```text
build.gradle
common/build.gradle
common/src/main/java/immersive_machinery/client/KeyBindings.java
common/src/main/java/immersive_machinery/client/gui/screen/BambooBeeScreen.java
common/src/main/java/immersive_machinery/entity/TunnelDigger.java
common/src/main/java/immersive_machinery/network/c2s/BambooBeeConfigurationUpdate.java
common/src/main/java/immersive_machinery/network/c2s/TunnelDiggerControlsUpdate.java
gradle.properties
neoforge/build.gradle
neoforge/src/main/resources/META-INF/neoforge.mods.toml
neoforge/src/main/resources/pack.mcmeta
settings.gradle
```

New: this checkpoint; neoforge/src/portTest/java/local_port_server_checks/{ServerChecks,ClientChecks}.java and its separate metadata. Complete list exported to local-26.3-jars/evidence/ImmersiveMachinery-source-changes.txt.
