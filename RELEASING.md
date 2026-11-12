Releasing
=========

### Prerequisite: Sonatype (Maven Central) Account

Create an account on the [Sonatype issues site][sonatype_issues]. Ask an existing publisher to open
an issue requesting publishing permissions for `com.squareup` projects.


Release Candidates
------------------

Cut a release candidate first when the change log has an entry marked `⚠ Breaking` or `Behavior change`, or when Kotlin or KotlinPoet changed.
Use the version format `X.Y.Z-RCnn`, starting at `RC01`, and follow the steps below.

After the release candidate is published:

1. Write the GitHub release notes for it.
   List each breaking change and behavior change in plain words, and link to [Trying a Release Candidate][upgrading].
2. Tell the main consumers that the release candidate is available.
3. Wait at least three business days for reports.
4. Fix the reported problems and cut `RCnn+1`, or cut the final release if there are no problems.


Cutting a JVM Release
---------------------

1. Update `CHANGELOG.md`.

   If the PGP signing key changed since the last release, announce the change in this release's
   entry. Include the new key ID and the new fingerprint. Also update `docs/signing_keys.md` to
   list the new key.

2. Set versions:

    ```
    export RELEASE_VERSION=X.Y.Z
    export NEXT_VERSION=X.Y.Z-SNAPSHOT
    ```

3. Update versions:

    ```
    sed -i "" \
      "s/VERSION_NAME=.*/VERSION_NAME=$RELEASE_VERSION/g" \
      `find . -name "gradle.properties"`
    ```

4. Tag the release and push to GitHub.

    ```
    git commit -am "Prepare for release $RELEASE_VERSION."
    git tag -a $RELEASE_VERSION -m "Version $RELEASE_VERSION"
    git push && git push --tags
    ```

5. Wait for [GitHub Actions][github_actions] to start building the release.

6. Prepare for ongoing development and push to GitHub.

    ```
    sed -i "" \
      "s/VERSION_NAME=.*/VERSION_NAME=$NEXT_VERSION/g" \
      `find . -name "gradle.properties"`
    git commit -am "Prepare next development version."
    git push
    ```

7. CI will release the artifacts and publish the website.

 [sonatype_issues]: https://issues.sonatype.org/
 [sonatype_nexus]: https://s01.oss.sonatype.org/
 [github_actions]: https://github.com/square/wire/actions
 [upgrading]: https://square.github.io/wire/upgrading/
