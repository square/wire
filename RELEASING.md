Releasing
=========

### Prerequisite: Sonatype (Maven Central) Account

Create an account on the [Sonatype issues site][sonatype_issues]. Ask an existing publisher to open
an issue requesting publishing permissions for `com.squareup` projects.


Cutting a JVM Release
---------------------

1. Update `CHANGELOG.md`.

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


Publishing the Swift CocoaPods
------------------------------

There are two Podspecs to publish to CocoaPods: the Swift Wire runtime and the Swift Wire compiler. The same version number should be used for both.

CocoaPods are published to the [trunk](https://blog.cocoapods.org/CocoaPods-Trunk/) repo, which is the main public repo for all CocoaPods. If you have not published Wire before then you'll need to [get set up to publish to trunk](https://guides.cocoapods.org/making/getting-setup-with-trunk.html), and be added as a publisher for the Wire Podspecs.

### Setting the Version

When publishing a new version, two things must be done:
1. The version must be tagged in Git. So if you're publishing version `4.0.0-alpha1`, then you'd check out the SHA you want to publish and run:
```
git tag 4.0.0-alpha1
git push origin refs/tags/4.0.0-alpha1
```

2. The version being published needs to be passed into the Podspecs. This is done by setting the `POD_VERSION` environment variable:
```
export POD_VERSION=4.0.0-alpha1
```

If publishing a release version (like `4.0.0` rather than `4.0.0-alpha1`) then setting the `POD_VERSION` is optional and it will be pulled automatically from `gradle.properties`.

### Publishing the Podspecs

After setting the version as described above, you can publish the two Podspecs by doing:

```
# Tests are currently failing, thus --skip-tests is required
pod trunk push Wire.podspec --skip-tests
```

and

```
pod trunk push WireCompiler.podspec
```
