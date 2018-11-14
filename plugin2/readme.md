features:
-generating by source folder from project structure tree context menu
-generating by java file from project structure tree context menu
-generating by java class from editor context menu
-generating by method of java class from editor context menu
-generation done by standard existing GP pipeline 
-status bar indicator
-output to defined test source folder
-merging output with existing files (adding new methods without overriding existing methods in target test file)
-plugin labels in bundle properties
-generating all profiles at once (open-pojo,manual,delegates)
-automatically remove unused imports
-add test dependencies to maven pom.xml from project structure tree context menu (only for pom.xml file)

notes:
-if there is no test source folder in module - generation will not be available (similar to standard JUnit plugin, where generated file is placed to the same folder, as source class)

todo:
-pack plugin to zip and test