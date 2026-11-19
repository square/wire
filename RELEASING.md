Releasing
=========

### Prerequisite: Sonatype (Maven Central) Account

Create an account on the [Sonatype issues site][sonatype_issues]. Ask an existing publisher to open
an issue requesting publishing permissions for `com.squareup` projects.


Cutting a JVM Release
---------------------

1. Update `CHANGELOG.md`.

   If the PGP signing key changed since the last release, announce the change in this release's
   entry. Include the new key ID and the new fingerprint. Also update `docs/signing_keys.md` to
   list the new key.

2. For a new minor version (X.Y.0), update the supported version in `SECURITY.md`.

3. Set versions:

    ```
    export RELEASE_VERSION=X.Y.Z
    export NEXT_VERSION=X.Y.Z-SNAPSHOT
    ```

4. Update versions:

    ```
    sed -i "" \
      "s/VERSION_NAME=.*/VERSION_NAME=$RELEASE_VERSION/g" \
      `find . -name "gradle.properties"`
    ```

5. Tag the release and push to GitHub.

    ```
    git commit -am "Prepare for release $RELEASE_VERSION."
    git tag -a $RELEASE_VERSION -m "Version $RELEASE_VERSION"
    git push && git push --tags
    ```

6. Wait for [GitHub Actions][github_actions] to start building the release.

7. Prepare for ongoing development and push to GitHub.

    ```
    sed -i "" \
      "s/VERSION_NAME=.*/VERSION_NAME=$NEXT_VERSION/g" \
      `find . -name "gradle.properties"`
    git commit -am "Prepare next development version."
    git push
    ```

8. CI will release the artifacts and publish the website.

 [sonatype_issues]: https://issues.sonatype.org/
 [sonatype_nexus]: https://s01.oss.sonatype.org/
 [github_actions]: https://github.com/square/wire/actions
