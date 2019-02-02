import java.util.List;
import java.util.ArrayList;
import jh61b.grader.TestResult; 
import java.util.Scanner;  //to read in file of diff results
import java.io.File;
import java.io.IOException;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;

/**
   Classs representing an autograder.
   It's main method is the running of the autograder
   and instances can be made to store all important information.
*/
public class Autograder {

   /**The list of all tests performed.*/
   private List<TestResult> allTestResults;
   
   /** The value of each test.*/
   private final double maxScore = 0.1;
   
   /**The current test number we are on.*/
   private int diffNum;
   
   /**
      The main class constructor.
      Initializes the list of all tests.
   */
   public Autograder() {
      this.allTestResults = new ArrayList<TestResult>();
      this.diffNum = 1;
   }
   
   
   /** Code to run at the end of test run. 
       @throws Exception fails to create json for a test 
    */
   public void testRunFinished() throws Exception {  
      /* Dump allTestResults to StdOut in JSON format. */
      ArrayList<String> objects = new ArrayList<String>();
      for (TestResult tr : this.allTestResults) {
         objects.add(tr.toJSON());
      }
      String testsJSON = String.join(",", objects);
      
      System.out.println("{" + String.join(",", new String[] {
               String.format("\"tests\": [%s]", testsJSON)}) + "}");
   }

   /**
    * Check if source file exists.
    * @param programName the program name
    * @return whether or not the source exists
    */
   public boolean testSourceExists(String programName) {
      boolean sourceExists = false;
      File source = new File(programName + ".java");
      TestResult trSourceFile = new TestResult(programName +
                                               " Source File Exists", 
                                               "Pre-Test",
                                               this.maxScore, 
                                               "visible");
      
      if (!source.exists() || source.isDirectory()) { // source not present
         trSourceFile.setScore(0);
         trSourceFile.addOutput("ERROR: file " + programName +
                                 ".java is not present!\n");
         trSourceFile.addOutput("\tCheck the spelling of your file name.\n");
         trSourceFile.addOutput("\tNo further autograder tests will be run for "
                                 + programName + ".\n");
      } else { // source present
         trSourceFile.setScore(this.maxScore);
         trSourceFile.addOutput("SUCCESS: file " + programName +
                                 ".java is present!\n");
         sourceExists = true;
      }
      this.allTestResults.add(trSourceFile);
      return sourceExists;
   }

   /** Function to test if a class compiles.
       @param programName the name of the java file to test
       @return whether the class compiled
    */
   public boolean testCompiles(String programName) {
      boolean passed = false;
      //File source = new File(programName + ".class");
      TestResult trCompilation = new TestResult(programName + " Compiles",
                                                 "Pre-Test", this.maxScore,
                                                "hidden");
      String fileName = programName + ".java";
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      int compilationResult = compiler.run(null, null, null, fileName);
      if (compilationResult != 0) {
         trCompilation.setScore(0);
         trCompilation.addOutput("ERROR: " + programName + 
                                  ".java did not compile!\n");
         trCompilation.addOutput("\tFix your code and re-submit!");      
      } 
      else {
         trCompilation.setScore(this.maxScore);
         trCompilation.addOutput("SUCCESS: " + programName + 
                                  ".java compiled successfully!\n");
         passed = true;
      }
      this.allTestResults.add(trCompilation);
      return passed;
   }
   
   
   /**
    * Checks if checkstyle passed.
    * @param programName the program name
    */
   public void testCheckstyle(String programName) {
      TestResult trCheck = new TestResult(programName + "Checkstyle Compliant",
                                          "Pre-Test",
                                           this.maxScore, "hidden");
      String checkstyle = "/autograder/source/checkstyle/";
      
      String result;
      try {
         String proc = "java -jar " + checkstyle + "checkstyle-6.10-all.jar" +
            " -c " + checkstyle + "check112.xml /autograder/source/" +
            programName + ".java";
         Process check = Runtime.getRuntime().exec(proc);
         check.waitFor();  
         Scanner s = new Scanner(check.getInputStream()).useDelimiter("\\A");
         result = s.hasNext() ? s.next() : "";
         //no problems reported in checkstylefile; it passed checkstyle
         if (result.equals("Starting audit...\nAudit done.\n")) {
            trCheck.setScore(this.maxScore);
            trCheck.addOutput("SUCCESS: " + programName +
                               " passed checkstyle with no warnings\n");
         }
         else {  //something in checkstylefile; it failed checkstyle
            trCheck.setScore(0);
            trCheck.addOutput("ERROR: " + programName +
                               " did not pass checkstyle." + 
                              " Results are below:\n" + result);
         }
         
      }  catch (IOException e) {
         return;
      } catch (InterruptedException e) {
         return;
      }
      this.allTestResults.add(trCheck);
   }
   
   /**
    * Runs Project 1A for the grader to use.
    */
   public void p1aRun() {
      TestResult trRun = new TestResult("Proj1A Run", "" + 1,
                                         this.maxScore, "hidden");
      
      String result;
      try {
         String procMain = "java Proj1A";
         Process mainProcess = Runtime.getRuntime().exec(procMain);
         mainProcess.waitFor();
         Scanner s = new Scanner(mainProcess.getInputStream())
            .useDelimiter("\\A");
         result = s.hasNext() ? s.next() : "";
         trRun.setScore(this.maxScore);
         if (result.contains("\\ ")) {
            trRun.addOutput("Run file seperately for " +
                            "output as it uses escaped characters in design");
         } else {
            trRun.addOutput("OUTPUT:\n" + result);
         }
      }  catch (IOException e) {
         return;
      } catch (InterruptedException e) {
         return;
      }
      this.allTestResults.add(trRun);
   }

   
   /**
      Runs a all the diff tests for a specific file.
      All input files are named: {Program_Name}_Test_#.in
      @param p the program to do diff tests on
   */
   public void diffTests(Program p) {
      
      for (int i = 0; i < p.testCount(); i++) {
         TestResult trDiff = new TestResult(p.name() + "Diff Test #" + i,
                                            "" + this.diffNum,
                                            this.maxScore, "hidden");
         this.diffNum++;
         String input = p.name() + "_Test_" + i + ".in";
         String exOut = p.name() + "_expected_" + i + ".out";
         String acOut = p.name() + "_" + i + ".out";
         String result;
         try {
            
            String procSample = "java " + p.name() +
               "Sample.java < " + input + " > " + exOut;
            Process sampleProcess = Runtime.getRuntime().exec(procSample);
            sampleProcess.waitFor();
            
            String procMain = "java " + p.name() + 
               ".java < " + input + " > " + acOut;
            Process mainProcess = Runtime.getRuntime().exec(procMain);
            mainProcess.waitFor();
            if (mainProcess.exitValue() != 0) {
               trDiff.setScore(0);
               trDiff.addOutput("ERROR: " + p.name() +
                                  " Caused a RuntimeError and crashed");
               return;
            }
            String procDiff = "diff " + exOut +
               " " + acOut + "-y --suppress-common-lines -W 250";
            Process diffProcess = Runtime.getRuntime().exec(procDiff);
            diffProcess.waitFor();
            
            Scanner s = new Scanner(diffProcess.getInputStream())
               .useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            if (diffProcess.exitValue() == 0) {
               trDiff.setScore(this.maxScore);
               trDiff.addOutput("SUCCESS: " + p.name() +
                                  " passed this diff test\n");
            }
            else { 
               trDiff.setScore(0);
               trDiff.addOutput("ERROR: " + p.name() +
                                  " differed from expected output." +
                                " Results are below:\n" + result);
            }
            
         }  catch (IOException e) {
            return;
         } catch (InterruptedException e) {
            return;
         }
         this.allTestResults.add(trDiff);
      }
   }
   

   /**
      Main method of the autograder.
      Runs all of the tests
      @param args the files and diff test counts
      @throws Exception When something goes wrong with a test
    */
   public static void main(String[] args) throws Exception {
      
      Autograder gr = new Autograder();
      if (args.length < 2 || (args.length % 2) != 0) {
         System.out.println("Missing Command Line Arguments");
         return;
      }
      int progCount = args.length / 2;
      Program[] programs = new Program[progCount];
      for (int i = 0; i < programs.length; i++) {
         programs[i] = new Program(args[2 * i], args[2 * i + 1]);
         if (!gr.testSourceExists(programs[i].name()) ||
             !gr.testCompiles(programs[i].name())) {
            programs[i].setExists(false);
         } else {
            gr.testCheckstyle(programs[i].name());
         }
      }
      for (int i = 0; i < programs.length; i++) {
         if (programs[i].exists()) {
            gr.diffTests(programs[i]);
         }
      }
      if (programs[0].exists()) {
         gr.p1aRun();
      }
      gr.testRunFinished();
   }


   /**
      Small sub class representing a Java program.
    */
   private static class Program {
      /**The name of the program.*/
      private String name;

      /**The number of cooresponding diff tests.*/
      private int testCount;
      
      /**Whether the file exists in the submission.*/
      private boolean exists;

      /**
         Public constructor of the class.
         @param newName the name of the program
         @param count the number of diff tests
       */
      Program(String newName, String count) {
         this.name = newName;
         this.testCount = Integer.parseInt(count);
         this.exists = true;
      }
      
      /**
         returns whether the file exists.
         @return exists
       */
      public boolean exists() {
         return this.exists;
      }

      /** Set whether a submission file exists.
          @param b whether it exists
       */
      public void setExists(boolean b) {
         this.exists = b;
      }

      /**
         Getter for the name.
         @return the name
       */
      public String name() {
         return this.name;
      }
      
      /**
         Getter for the test count.
         @return the test count
       */
      public int testCount() {
         return this.testCount;
      }
   }
}