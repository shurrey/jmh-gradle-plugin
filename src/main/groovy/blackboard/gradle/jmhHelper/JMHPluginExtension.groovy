package blackboard.gradle.jmhHelper;


class JMHPluginExtension {
  //Options that get exposed to the command line are here?i

  //JMH options can also be set by annotation, which provides sensible defaults to a BenchmarkTest.  
  public String outputFile;

  public String custArgs;
  
  

  /*public final String HELP_OPT = "-h";
  //Requires a Mode be specified.Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
  public final String BENCHMARK_MODE = "-bm";
  //Number of benchmark calls per operation. Requires an int specified,
  public final String BATCH_SIZE = "-bs"; */



}