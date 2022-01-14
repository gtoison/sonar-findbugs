# Release procedure

When you release fixed version of SonarQube SpotBugs Plugin, please follow these procedures.

* create topic branch from `master` branch

```
git checkout -b master-release
```

* Make sure profile XMLs are updated. See [`generate_progiles/README.md`](generate_progiles/README.md) for detail.

* change version number in `pom.xml` to stable version (e.g. `1.2.3`), then commit changes

```
mvn versions:set -DnewVersion=1.2.3
```

* change version number in `pom.xml` to next development SNAPSHOT version (e.g. `1.2.4-SNAPSHOT`), then commit changes

```
mvn versions:set -DnewVersion=1.2.4-SNAPSHOT
```

* push your topic branch and propose a pull request
* after merging your pull request, create a GitHub Release with the commit which has stable version in `pom.xml`. The name of tag should have no prefix nor suffix, e.g. `4.0.0`

## Release to Maven Central

When we push tag, the build result on Travis CI will be deployed to [Sonatype Nexus](https://oss.sonatype.org/).
Check [Sonatype official page](http://central.sonatype.org/pages/apache-maven.html) for detail.

## Release to SonarQube Marketplace

[sonar-update-center-action](https://github.com/KengoTODA/sonar-update-center-action/) will handle necessary procedures. Please confirm that the PR is made in [SonarSource/sonar-update-center-properties](https://github.com/SonarSource/sonar-update-center-properties/), and a topic is posted to [the community forum](https://community.sonarsource.com/c/plugins).
