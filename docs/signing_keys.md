Signing Keys
============

We sign all Wire artifacts that we publish to Maven Central with a PGP key. Each artifact has a
detached signature file with the `.asc` extension. You can use these signatures to verify that an
artifact is authentic.

Current Key
-----------

| Field       | Value                                                  |
| ----------- | ------------------------------------------------------ |
| User ID     | `Block Open Source Releases <oss-releases@block.xyz>`  |
| Key ID      | `793FD5751A0F0780`                                     |
| Fingerprint | `1D21 7F84 75EE E9F1 9AB8 DD6B 793F D575 1A0F 0780`    |
| Type        | `ed25519`                                              |
| Created     | 2024-11-18                                             |
| Expires     | 2034-11-16                                             |

This key signs all Wire 6.x and 7.x releases.

Download the key from a public keyserver:

```
gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys 793FD5751A0F0780
```

Verify an Artifact Manually
---------------------------

Download an artifact and its `.asc` signature from Maven Central. Then verify the signature:

```
gpg --verify wire-runtime-7.0.3.jar.asc wire-runtime-7.0.3.jar
```

Confirm that the output reports a good signature. Confirm that the primary key fingerprint matches
the fingerprint in the table above.

Gradle Dependency Verification
------------------------------

Gradle can verify the signature of each dependency during a build. See the
[Gradle documentation][gradle_verification] for the full setup. Add the Wire key to the trusted
keys in `gradle/verification-metadata.xml`:

```xml
<trusted-keys>
   <trusted-key id="1D217F8475EEE9F19AB8DD6B793FD5751A0F0780" group="com.squareup.wire"/>
</trusted-keys>
```

Key Changes
-----------

When the signing key changes, we announce the new key in the [change log](changelog.md) entry for
the first release that the new key signs. The announcement includes the new key ID and the new
fingerprint.

[gradle_verification]: https://docs.gradle.org/current/userguide/dependency_verification.html
