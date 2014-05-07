package com.blackboard.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * This task is used to run JMH benchmark files. By default, this task runs every
 * benchmark for a project in the src/benchmark/java/... folder.
 */
public class BenchmarkJmhTask extends JavaExec {

  private static final String JMH_RUNNER = "org.openjdk.jmh.Main";
  //private static final String OS_TYPE = System.getProperty("os.name").contains("windows")? "windows" : "linux" ;
  private String defaultOutputFile = String.valueOf(getProject().getBuildDir()) + File.separator + "jmh-output.txt";

  private final HashSet<String> VALID_JMH_ARGS = new HashSet<>(Arrays.asList("-bm", "-bs", "-e","-f", "-foe","-gc", "-h", "-i", "-jvm", "-jvmArgs", "-jvmArgsAppend", "-jvmArgsPrepend", "-l", "-lprof", "-lrf", "-o", "-p", "-prof", "-r", "-rf", "-rff", "-si", "-t","-tg","-tu","-v", "-wbs", "-wf", "-wi", "-wm", "-wmb"));

  @TaskAction
  public void benchmarkJmh() {
    this.setMain(JMH_RUNNER);
    /* Sets the classpath for the JMH runner. This requires the output of the benchmark sourceSet
     * as well as the runtime-classpath of the benchmarks. */
    JavaPluginConvention jpc = this.getProject().getConvention().getPlugin(JavaPluginConvention.class);
    FileCollection fcClasspath = jpc.getSourceSets().getByName("benchmark").getRuntimeClasspath();
    this.setClasspath(fcClasspath);
    //Sends arguments defined in the gradle syntax of -P to the JMH runner. Example: -P-o="/my_path/text.txt"
    this.setArgs(processJmhArgs());
    //Sets the JVM specific args.

    this.setJvmArgs(processJVMargs());
    this.exec();
  }

  private ArrayList<String> processJVMargs(){
    ArrayList<String> jvmArgs = new ArrayList<>();
    String jvmProp = (String) this.getProject().getProperties().get("-jvmArgs");
    if (null != jvmProp) {
      jvmArgs.addAll(Arrays.asList(jvmProp.split(" ")));
    }
    return jvmArgs;
  }

  private ArrayList<String> processJmhArgs(){
    ArrayList<String> toJmhRunner = new ArrayList<>();
    Project pj = this.getProject();
    HashSet<String> props = new HashSet<>(pj.getProperties().keySet());
    /* Changes props to be the set-intersection of all project properties and VALID_JMH_ARGS. This gives me only the
     * the arguments passed into the project that are JMH arguments. (As opposed to all project arguments that may
     * exist from gradle doing its magic) */
    props.retainAll(VALID_JMH_ARGS);


    /* Adds args and their values to the list to be given to JMHRunner. */
    props.remove("-o");
    props.remove("help");
    props.remove("-jvmArgs");
    props.remove("-jvmArgsAppend");
    props.remove("-jvmArgsPrepend");
    for (String prop : props){
        toJmhRunner.add(prop);
        toJmhRunner.add((String) pj.getProperties().get(prop));
    }

    //TODO: The create logic to change the output to a safe location other than the project build directory.
    if (pj.hasProperty("-o")){
      String fName = pj.getBuildDir() + File.separator + pj.getProperties().get("-o");
      new File(fName);
      toJmhRunner.add("-o");
      toJmhRunner.add(fName);
    } else {
      new File(defaultOutputFile).getParentFile().mkdirs();
      toJmhRunner.addAll(new ArrayList<>(Arrays.asList("-o", defaultOutputFile)));
    }

    //Help is displayed in the console, clears all other options.
    if (pj.hasProperty("help")){
      displayUsage();
      toJmhRunner.clear();
      toJmhRunner.add("-h");
    }

    return toJmhRunner;
  }

  private void displayUsage(){
    System.out.println("This task depends on the fact that all benchmarks are compilable, valid, java jmh benchmarks.");
    System.out.println("For this task to function, benchmarks must be placed in src/benchmark/java/<your own folder structure here>");
    System.out.println("Arguments to this task are defined with the gradle -P syntax.");
    System.out.println("By default, the output of JMH benchmarks are located in jmh-output.txt in the build directory of this project.");
  }

}
