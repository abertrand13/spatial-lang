name := "apps"

publishArtifact := false
trapExit := false

scalaSource in Compile := baseDirectory(_/ "src").value
resourceDirectory in Compile :=  baseDirectory(_/ "resources").value
