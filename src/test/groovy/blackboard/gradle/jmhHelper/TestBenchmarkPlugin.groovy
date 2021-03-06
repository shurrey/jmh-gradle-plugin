package blackboard.gradle.jmhHelper

import com.blackboard.gradle.BenchmarkJmhTask
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.tasks.SourceSet
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;


class TestBenchmarkPlugin {

  Project _project;

  @Before
  void setUp(){
    _project = ProjectBuilder.builder().build();
    _project.apply plugin: 'jmh';
  }
	
  @Test
  public void checkPluginAppliedToProject() {
    assertTrue("Project should have JMHPlugin", _project.getPlugins().hasPlugin("jmh"));
  }
  
  @Test
  public void checkBenchmarkJMHTaskIsAddedWhenPluginIsApplied() {
    assertNotNull( _project.getTasks().getByName("benchmarkJmh") );
    assertTrue( "Project is missing task of type BenchmarkJmhTask", _project.getTasks().getByName("benchmarkJmh") instanceof BenchmarkJmhTask );
  }

  /* This test is pedantic, I created it to increase my understanding of how the _project.apply plugin mechanism works.
     From this, I can see that invoking JMHPlugin causes the JavaPlugin to be applied; this is the desired behavior, as the
     JMHPlugin uses a sourceSet. SourceSets can only exist after invoking the JavaPlugin. */
  @Test
  public void checkThatJavaPluginGetsAppliedWhenUsingJMHPlugin(){
    assertTrue("Project should have JavaPlugin", _project.getPlugins().hasPlugin("java"));
  }
  
  @Test
  public void checkBenchmarkSourceSet(){
    /* sourceSets, is an object of type SourceSetContainer, that appears after invoking apply JMHPlugin.
       This object is created (inherited?) from the java plugin which JMHPlugin invokes. */
    SourceSet tar = null;
    for (SourceSet s : _project.sourceSets){
      if (s.getName().equals("benchmark")) {
        tar = s;
        break;
      }
    }
    assertNotNull("Expected benchmark sourceSet to be present", tar);

    /* Verifying that the benchmark sourceSet actually refers to a valid location for JMH benchmarks.
       By our convention, this will be src/benchmark/java/<more folders forming packaage names here>.
       This test is currently not robust, it is just looking for "benchmark" in a filepath somewhere, and 
       can easily be fooled if someone makes a package named benchmark. This is because I don't want to do
       OS detection to determine filepaths in other operating systems. */

    SourceDirectorySet sd = tar.getAllSource();
    HashSet<File> fs = sd.getSrcDirs();
    boolean valid_sourceSet = false;
    for (File f : fs ) {
      if (f.getPath().contains("benchmark")) {
        valid_sourceSet = true;
        break;
      }
    }
    assertTrue(valid_sourceSet);
  }

  @Test
  public void testSourceSetHasRuntimeClasspath() {
    SourceSet s = _project.sourceSets.benchmark;
    assertNotNull(s); 
    assertFalse(s.runtimeClasspath.isEmpty());
  }

  @Test
  public void testDescriptionOfTask(){
    Task t = null;
    try {
         t = _project.getTasks().getByName("benchmarkJmh");
    } catch (UnknownTaskException e){
      fail("Task benchmarkJmh could not be found by name");
    }
    assertNotNull(t);
    assertNotNull(t.getDescription());
  }

  @Test
  public void testApplyEclipsePlugin(){
    Project pj = ProjectBuilder.builder().build();
    pj.apply plugin: 'eclipse';
    pj.apply plugin: 'jmh';
    assertTrue( "Expected Eclipse Plugin to be applied to the project.",pj.getPlugins().hasPlugin("eclipse") );
    assertTrue( "Expected Jmh plugin to be applied to the project", pj.getPlugins().hasPlugin("jmh") );
    pj.projectEvaluationBroadcaster.afterEvaluate(pj, null)

    EclipsePlugin ep = pj.getPlugins().getPlugin(EclipsePlugin.class);
    EclipseClasspath eCp = ep.getModel().getClasspath();
    boolean benchmarkCompile = false;
    boolean benchmarkRuntime = false;

    for (DefaultConfiguration dc : eCp.getPlusConfigurations() ) {
        String s =  dc.getName();
        if ( s.equals("benchmarkCompile") ){
          benchmarkCompile = true;
        }
        if ( s.equals("benchmarkRuntime") ){
          benchmarkRuntime = true;
        }
    }
    assertTrue( "Expected Eclipse classpath to contain benchmarkCompile entries", benchmarkCompile );
    assertTrue( "Expected Eclipse classpath to contain benchmarkRuntime entries", benchmarkRuntime );
  }

  @Test
  public void testApplyIdeaPlugin(){
    Project pj = ProjectBuilder.builder().build();
    pj.apply plugin: 'idea';
    pj.apply plugin: 'jmh';
    assertTrue( "Idea plugin is missing from project",pj.getPlugins().hasPlugin("idea") );
    assertTrue( "JMH plugin is missing from project", pj.getPlugins().hasPlugin("jmh") );
    pj.projectEvaluationBroadcaster.afterEvaluate(pj, null)
    IdeaPlugin ideaPlugin = pj.getPlugins().getPlugin(IdeaPlugin.class);
    IdeaModule ideaModule = ideaPlugin.getModel().getModule();

    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, ArrayList<DefaultConfiguration> >>> scopes = ideaModule.getScopes();

    LinkedHashMap<String, LinkedHashMap<String, ArrayList<DefaultConfiguration> >> testScope =  scopes.get("TEST");

    assertNotNull(testScope);

    ArrayList<DefaultConfiguration>  plus = testScope.get("plus");

    assertNotNull(plus);

    boolean benchmarkCompile = false;
    boolean benchmarkRuntime = false;

    for (DefaultConfiguration dc : plus ) {
      String s =  dc.getName();
      if ( s.equals("benchmarkCompile") ){
        benchmarkCompile = true;
      }
      if ( s.equals("benchmarkRuntime") ){
        benchmarkRuntime = true;
      }
    }

    assertTrue(benchmarkCompile);
    assertTrue(benchmarkRuntime);
  }
}
