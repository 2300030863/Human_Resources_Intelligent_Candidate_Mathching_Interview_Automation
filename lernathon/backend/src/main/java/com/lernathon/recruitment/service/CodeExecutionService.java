package com.lernathon.recruitment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lernathon.recruitment.dto.CompileResponse;
import com.lernathon.recruitment.dto.RunCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class CodeExecutionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long TIMEOUT_SECONDS = 5;

    public static class TestCase {
        public String input;
        public String expectedOutput;

        public TestCase() {}
        
        public TestCase(String input, String expectedOutput) {
            this.input = input;
            this.expectedOutput = expectedOutput;
        }
    }

    public static class TestResult {
        public int totalTests;
        public int passedTests;
        public double passPercentage;
        public List<String> failedTests;

        public TestResult(int total, int passed) {
            this.totalTests = total;
            this.passedTests = passed;
            this.passPercentage = total > 0 ? (passed * 100.0 / total) : 0;
            this.failedTests = new ArrayList<>();
        }
    }

    /**
     * Execute candidate code against test cases
     * 
     * @param candidateCode The code submitted by candidate
     * @param testCasesJson JSON string containing test cases
     * @param language Programming language (java, python, javascript)
     * @return TestResult with pass/fail information
     */
    public TestResult executeTestCases(String candidateCode, String testCasesJson, String language) {
        log.info("Executing test cases for {} code", language);
        
        if (testCasesJson == null || testCasesJson.trim().isEmpty()) {
            log.warn("No test cases provided, returning 0/0");
            return new TestResult(0, 0);
        }

        try {
            List<TestCase> testCases = objectMapper.readValue(testCasesJson, 
                    new TypeReference<List<TestCase>>() {});
            return executeTestCases(candidateCode, testCases, language);
        } catch (Exception e) {
            log.error("Error parsing test cases JSON", e);
            return new TestResult(1, 0);
        }
    }

    /**
     * Execute candidate code against test cases (direct list)
     * 
     * @param candidateCode The code submitted by candidate
     * @param testCases List of test cases
     * @param language Programming language (java, python, javascript)
     * @return TestResult with pass/fail information
     */
    public TestResult executeTestCases(String candidateCode, List<TestCase> testCases, String language) {
        log.info("Executing test cases for {} code", language);
        
        if (testCases == null || testCases.isEmpty()) {
            log.warn("No test cases provided, returning 0/0");
            return new TestResult(0, 0);
        }

        log.info("Found {} test cases to execute", testCases.size());

        // Execute based on language
        if ("java".equalsIgnoreCase(language)) {
            return executeJavaCode(candidateCode, testCases);
        } else if ("python".equalsIgnoreCase(language)) {
            return executePythonCode(candidateCode, testCases);
        } else if ("javascript".equalsIgnoreCase(language)) {
            return executeJavaScriptCode(candidateCode, testCases);
        } else {
            log.warn("Unsupported language: {}", language);
            // For unsupported languages, use simple length check
            return candidateCode.trim().length() > 50 ? 
                    new TestResult(1, 1) : new TestResult(1, 0);
        }
    }

    private TestResult executeJavaCode(String code, List<TestCase> testCases) {
        int passed = 0;
        TestResult result = new TestResult(testCases.size(), 0);

        try {
            // Real Java execution with compilation and running
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                try {
                    String output = executeJavaWithInput(code, testCase.input);
                    if (output.trim().equals(testCase.expectedOutput.trim())) {
                        passed++;
                        log.info("Test case {} passed", i + 1);
                    } else {
                        log.info("Test case {} failed. Expected: '{}', Got: '{}'", 
                                i + 1, testCase.expectedOutput.trim(), output.trim());
                        result.failedTests.add("Test " + (i + 1) + ": Expected '" + 
                                testCase.expectedOutput + "' but got '" + output + "'");
                    }
                } catch (Exception e) {
                    log.error("Test case {} threw exception: {}", i + 1, e.getMessage());
                    result.failedTests.add("Test " + (i + 1) + ": Exception - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error executing Java code", e);
        }

        result.passedTests = passed;
        result.passPercentage = testCases.size() > 0 ? (passed * 100.0 / testCases.size()) : 0;
        return result;
    }

    private String executeJavaWithInput(String code, String input) throws Exception {
        log.info("Executing Java code with input: {}", input);
        
        Path tempDir = Files.createTempDirectory("java_exec_");
        String className = "Solution";
        Path javaFile = tempDir.resolve(className + ".java");
        
        try {
            // Wrap code in a class if it's just a method/snippet
            String fullCode = prepareJavaCode(code, className);
            Files.write(javaFile, fullCode.getBytes());
            
            log.info("Compiling Java code...");
            
            // Compile the Java file
            Process compileProcess = new ProcessBuilder("javac", javaFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            if (!compileProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                compileProcess.destroyForcibly();
                throw new RuntimeException("Compilation timeout - took longer than " + TIMEOUT_SECONDS + " seconds");
            }
            
            if (compileProcess.exitValue() != 0) {
                String error = new String(compileProcess.getInputStream().readAllBytes());
                log.error("Java compilation failed: {}", error);
                throw new RuntimeException("Compilation error: " + error);
            }
            
            log.info("Running compiled Java code...");
            
            // Run the compiled class
            Process runProcess = new ProcessBuilder("java", "-cp", tempDir.toString(), className)
                    .directory(tempDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            
            // Provide input with newline
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    String inputWithNewline = input.endsWith("\n") ? input : input + "\n";
                    os.write(inputWithNewline.getBytes());
                    os.flush();
                }
            }
            
            // Wait for execution with timeout
            if (!runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                throw new RuntimeException("Execution timeout - program took longer than " + TIMEOUT_SECONDS + " seconds");
            }
            
            // Get output and error
            String output = new String(runProcess.getInputStream().readAllBytes());
            String error = new String(runProcess.getErrorStream().readAllBytes());
            
            log.info("Java execution completed. Exit code: {}", runProcess.exitValue());
            
            if (runProcess.exitValue() != 0) {
                log.error("Java execution failed: {}", error);
                throw new RuntimeException("Runtime error: " + (error.isEmpty() ? "Unknown error" : error));
            }
            
            return output.trim();
            
        } catch (Exception e) {
            log.error("Exception during Java execution: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Clean up temp files
            deleteDirectory(tempDir);
        }
    }
    
    private String prepareJavaCode(String code, String className) {
        // If code already contains a class definition, use it as is
        if (code.contains("class ") || code.contains("public class")) {
            // Replace any existing class name with our standard one
            return code.replaceAll("(public\\s+)?class\\s+\\w+", "public class " + className);
        }
        
        // If it's just a method, wrap it in a class
        if (code.contains("public ") && (code.contains("static ") || code.contains("void ") || code.contains("int ") || code.contains("String "))) {
            return "public class " + className + " {\n" + code + "\n}";
        }
        
        // If it's just code snippets, wrap in a main method and class
        return "public class " + className + " {\n" +
               "    public static void main(String[] args) {\n" +
               "        " + code.replaceAll("\n", "\n        ") + "\n" +
               "    }\n" +
               "}";
    }

    private TestResult executePythonCode(String code, List<TestCase> testCases) {
        int passed = 0;
        TestResult result = new TestResult(testCases.size(), 0);

        try {
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                try {
                    String output = executePythonWithInput(code, testCase.input);
                    if (output.trim().equals(testCase.expectedOutput.trim())) {
                        passed++;
                        log.info("Test case {} passed", i + 1);
                    } else {
                        log.info("Test case {} failed. Expected: '{}', Got: '{}'", 
                                i + 1, testCase.expectedOutput.trim(), output.trim());
                        result.failedTests.add("Test " + (i + 1) + ": Expected '" + 
                                testCase.expectedOutput + "' but got '" + output + "'");
                    }
                } catch (Exception e) {
                    log.error("Test case {} threw exception: {}", i + 1, e.getMessage());
                    result.failedTests.add("Test " + (i + 1) + ": Exception - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error executing Python code", e);
        }

        result.passedTests = passed;
        result.passPercentage = testCases.size() > 0 ? (passed * 100.0 / testCases.size()) : 0;
        return result;
    }

    private String executePythonWithInput(String code, String input) throws Exception {
        log.info("Executing Python code with input: {}", input);
        
        Path tempDir = Files.createTempDirectory("python_exec_");
        Path pythonFile = tempDir.resolve("solution.py");
        
        try {
            Files.write(pythonFile, code.getBytes());
            
            // Try python3 first, then python (for Windows compatibility)
            String pythonCmd = findPythonCommand();
            
            log.info("Using Python command: {}", pythonCmd);
            
            // Run the Python script
            Process runProcess = new ProcessBuilder(pythonCmd, pythonFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            
            // Provide input with newline for input() function
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    // Ensure input ends with newline for Python's input() to work
                    String inputWithNewline = input.endsWith("\n") ? input : input + "\n";
                    os.write(inputWithNewline.getBytes());
                    os.flush();
                }
            }
            
            // Wait for execution with timeout
            if (!runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                throw new RuntimeException("Execution timeout - program took longer than " + TIMEOUT_SECONDS + " seconds");
            }
            
            // Get output and error
            String output = new String(runProcess.getInputStream().readAllBytes());
            String error = new String(runProcess.getErrorStream().readAllBytes());
            
            log.info("Python execution completed. Exit code: {}, Output length: {}, Error length: {}", 
                    runProcess.exitValue(), output.length(), error.length());
            
            if (runProcess.exitValue() != 0) {
                log.error("Python execution failed with error: {}", error);
                throw new RuntimeException("Runtime error: " + (error.isEmpty() ? "Unknown error" : error));
            }
            
            return output.trim();
            
        } catch (Exception e) {
            log.error("Exception during Python execution: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Clean up temp files
            deleteDirectory(tempDir);
        }
    }
    
    private String findPythonCommand() {
        // Try python3 first (Linux/Mac), then python (Windows)
        for (String cmd : new String[]{"python3", "python"}) {
            try {
                Process process = new ProcessBuilder(cmd, "--version")
                        .redirectErrorStream(true)
                        .start();
                if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    String version = new String(process.getInputStream().readAllBytes());
                    log.info("Found Python: {} - {}", cmd, version.trim());
                    return cmd;
                }
            } catch (Exception e) {
                log.debug("Python command '{}' not found: {}", cmd, e.getMessage());
            }
        }
        // Default to python and let it fail with a clear error message
        log.warn("Python not found in system PATH. Using 'python' as default.");
        return "python";
    }

    private TestResult executeJavaScriptCode(String code, List<TestCase> testCases) {
        int passed = 0;
        TestResult result = new TestResult(testCases.size(), 0);

        try {
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                try {
                    String output = executeJavaScriptWithInput(code, testCase.input);
                    if (output.trim().equals(testCase.expectedOutput.trim())) {
                        passed++;
                        log.info("Test case {} passed", i + 1);
                    } else {
                        log.info("Test case {} failed. Expected: '{}', Got: '{}'", 
                                i + 1, testCase.expectedOutput.trim(), output.trim());
                        result.failedTests.add("Test " + (i + 1) + ": Expected '" + 
                                testCase.expectedOutput + "' but got '" + output + "'");
                    }
                } catch (Exception e) {
                    log.error("Test case {} threw exception: {}", i + 1, e.getMessage());
                    result.failedTests.add("Test " + (i + 1) + ": Exception - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error executing JavaScript code", e);
        }

        result.passedTests = passed;
        result.passPercentage = testCases.size() > 0 ? (passed * 100.0 / testCases.size()) : 0;
        return result;
    }

    private String executeJavaScriptWithInput(String code, String input) throws Exception {
        log.info("Executing JavaScript code with input: {}", input);
        
        Path tempDir = Files.createTempDirectory("js_exec_");
        Path jsFile = tempDir.resolve("solution.js");
        
        try {
            Files.write(jsFile, code.getBytes());
            
            log.info("Running JavaScript code with Node.js...");
            
            // Run the JavaScript file with Node.js
            Process runProcess = new ProcessBuilder("node", jsFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            
            // Provide input with newline
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    String inputWithNewline = input.endsWith("\n") ? input : input + "\n";
                    os.write(inputWithNewline.getBytes());
                    os.flush();
                }
            }
            
            // Wait for execution with timeout
            if (!runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                throw new RuntimeException("Execution timeout - program took longer than " + TIMEOUT_SECONDS + " seconds");
            }
            
            // Get output and error
            String output = new String(runProcess.getInputStream().readAllBytes());
            String error = new String(runProcess.getErrorStream().readAllBytes());
            
            log.info("JavaScript execution completed. Exit code: {}", runProcess.exitValue());
            
            if (runProcess.exitValue() != 0) {
                log.error("JavaScript execution failed: {}", error);
                throw new RuntimeException("Runtime error: " + (error.isEmpty() ? "Unknown error" : error));
            }
            
            return output.trim();
            
        } catch (Exception e) {
            log.error("Exception during JavaScript execution: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Clean up temp files
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * Recursively delete a directory and its contents
     */
    private void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete {}: {}", path, e.getMessage());
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to clean up directory {}: {}", directory, e.getMessage());
        }
    }

    /**
     * Compile code without running it - for syntax checking
     */
    public CompileResponse compileCode(String code, String language) {
        log.info("Compiling {} code", language);
        
        try {
            if ("java".equalsIgnoreCase(language)) {
                return compileJavaCode(code);
            } else if ("python".equalsIgnoreCase(language)) {
                return compilePythonCode(code);
            } else if ("javascript".equalsIgnoreCase(language)) {
                return compileJavaScriptCode(code);
            } else {
                return CompileResponse.builder()
                        .success(false)
                        .status("FAILED")
                        .message("Unsupported language: " + language)
                        .build();
            }
        } catch (Exception e) {
            log.error("Compilation error", e);
            return CompileResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("Compilation failed")
                    .error(e.getMessage())
                    .build();
        }
    }

    private CompileResponse compileJavaCode(String code) throws Exception {
        Path tempDir = Files.createTempDirectory("java_compile_");
        String className = "Solution";
        Path javaFile = tempDir.resolve(className + ".java");
        
        try {
            String fullCode = prepareJavaCode(code, className);
            Files.write(javaFile, fullCode.getBytes());
            
            // Compile
            Process compileProcess = new ProcessBuilder("javac", javaFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            if (!compileProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                compileProcess.destroyForcibly();
                return CompileResponse.builder()
                        .success(false)
                        .status("FAILED")
                        .message("Compilation timeout")
                        .build();
            }
            
            String output = new String(compileProcess.getInputStream().readAllBytes());
            
            if (compileProcess.exitValue() != 0) {
                return CompileResponse.builder()
                        .success(false)
                        .status("FAILED")
                        .message("Compilation failed")
                        .error(output)
                        .build();
            }
            
            return CompileResponse.builder()
                    .success(true)
                    .status("SUCCESS")
                    .message("Compiled successfully ✓")
                    .build();
                    
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private CompileResponse compilePythonCode(String code) {
        // Python is interpreted, so we just check for syntax errors
        try {
            Path tempDir = Files.createTempDirectory("python_compile_");
            Path pythonFile = tempDir.resolve("solution.py");
            
            try {
                Files.write(pythonFile, code.getBytes());
                
                String pythonCmd = findPythonCommand();
                
                // Use -m py_compile to check syntax
                Process compileProcess = new ProcessBuilder(pythonCmd, "-m", "py_compile", pythonFile.toString())
                        .directory(tempDir.toFile())
                        .redirectErrorStream(true)
                        .start();
                
                if (!compileProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    compileProcess.destroyForcibly();
                    return CompileResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .message("Syntax check timeout")
                            .build();
                }
                
                String output = new String(compileProcess.getInputStream().readAllBytes());
                
                if (compileProcess.exitValue() != 0) {
                    return CompileResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .message("Syntax error")
                            .error(output)
                            .build();
                }
                
                return CompileResponse.builder()
                        .success(true)
                        .status("SUCCESS")
                        .message("Syntax check passed ✓")
                        .build();
                        
            } finally {
                deleteDirectory(tempDir);
            }
        } catch (Exception e) {
            return CompileResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("Syntax check failed")
                    .error(e.getMessage())
                    .build();
        }
    }

    private CompileResponse compileJavaScriptCode(String code) {
        // JavaScript is interpreted, check syntax using Node.js
        try {
            Path tempDir = Files.createTempDirectory("js_compile_");
            Path jsFile = tempDir.resolve("solution.js");
            
            try {
                Files.write(jsFile, code.getBytes());
                
                // Use node --check to verify syntax
                Process checkProcess = new ProcessBuilder("node", "--check", jsFile.toString())
                        .directory(tempDir.toFile())
                        .redirectErrorStream(true)
                        .start();
                
                if (!checkProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    checkProcess.destroyForcibly();
                    return CompileResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .message("Syntax check timeout")
                            .build();
                }
                
                String output = new String(checkProcess.getInputStream().readAllBytes());
                
                if (checkProcess.exitValue() != 0) {
                    return CompileResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .message("Syntax error")
                            .error(output)
                            .build();
                }
                
                return CompileResponse.builder()
                        .success(true)
                        .status("SUCCESS")
                        .message("Syntax check passed ✓")
                        .build();
                        
            } finally {
                deleteDirectory(tempDir);
            }
        } catch (Exception e) {
            return CompileResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("Syntax check failed")
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Run code with custom input
     */
    public RunCodeResponse runCode(String code, String input, String language) {
        log.info("Running {} code with custom input", language);
        
        long startTime = System.currentTimeMillis();
        
        try {
            if ("java".equalsIgnoreCase(language)) {
                return runJavaCodeWithInput(code, input, startTime);
            } else if ("python".equalsIgnoreCase(language)) {
                return runPythonCodeWithInput(code, input, startTime);
            } else if ("javascript".equalsIgnoreCase(language)) {
                return runJavaScriptCodeWithInput(code, input, startTime);
            } else {
                return RunCodeResponse.builder()
                        .status("FAILED")
                        .output("")
                        .error("Unsupported language: " + language)
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
        } catch (Exception e) {
            log.error("Execution error", e);
            return RunCodeResponse.builder()
                    .status("FAILED")
                    .output("")
                    .error("Execution failed: " + e.getMessage())
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
        }
    }

    private RunCodeResponse runJavaCodeWithInput(String code, String input, long startTime) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("java_run_");
            String className = "Solution";
            Path javaFile = tempDir.resolve(className + ".java");
            
            String fullCode = prepareJavaCode(code, className);
            Files.write(javaFile, fullCode.getBytes());
            
            // Compile
            Process compileProcess = new ProcessBuilder("javac", javaFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            if (!compileProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                compileProcess.destroyForcibly();
                return RunCodeResponse.builder()
                        .status("TIMEOUT")
                        .output("")
                        .error("Compilation timeout")
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            if (compileProcess.exitValue() != 0) {
                String error = new String(compileProcess.getInputStream().readAllBytes());
                return RunCodeResponse.builder()
                        .status("FAILED")
                        .output("")
                        .error("Compilation error: " + error)
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            // Run
            Process runProcess = new ProcessBuilder("java", "-cp", tempDir.toString(), className)
                    .directory(tempDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            
            // Provide input and close stdin
            try (OutputStream os = runProcess.getOutputStream()) {
                if (input != null && !input.isEmpty()) {
                    os.write((input + "\n").getBytes());
                    os.flush();
                }
            }
            
            if (!runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                return RunCodeResponse.builder()
                        .status("TIMEOUT")
                        .output("")
                        .error("Execution timeout (>" + TIMEOUT_SECONDS + "s)")
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            String output = new String(runProcess.getInputStream().readAllBytes());
            String error = new String(runProcess.getErrorStream().readAllBytes());
            
            if (runProcess.exitValue() != 0) {
                return RunCodeResponse.builder()
                        .status("FAILED")
                        .output(output)
                        .error("Runtime error: " + error)
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            return RunCodeResponse.builder()
                    .status("SUCCESS")
                    .output(output)
                    .error(null)
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
                    
        } catch (Exception e) {
            return RunCodeResponse.builder()
                    .status("FAILED")
                    .output("")
                    .error(e.getMessage())
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private RunCodeResponse runPythonCodeWithInput(String code, String input, long startTime) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("python_run_");
            Path pythonFile = tempDir.resolve("solution.py");
            
            Files.write(pythonFile, code.getBytes());
            
            String pythonCmd = findPythonCommand();
            
            Process runProcess = new ProcessBuilder(pythonCmd, pythonFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            
            // Provide input and close stdin
            try (OutputStream os = runProcess.getOutputStream()) {
                if (input != null && !input.isEmpty()) {
                    os.write((input + "\n").getBytes());
                    os.flush();
                }
            }
            
            if (!runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                return RunCodeResponse.builder()
                        .status("TIMEOUT")
                        .output("")
                        .error("Execution timeout (>" + TIMEOUT_SECONDS + "s)")
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            String output = new String(runProcess.getInputStream().readAllBytes());
            String error = new String(runProcess.getErrorStream().readAllBytes());
            
            if (runProcess.exitValue() != 0) {
                return RunCodeResponse.builder()
                        .status("FAILED")
                        .output(output)
                        .error("Runtime error: " + error)
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            return RunCodeResponse.builder()
                    .status("SUCCESS")
                    .output(output)
                    .error(null)
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
                    
        } catch (Exception e) {
            return RunCodeResponse.builder()
                    .status("FAILED")
                    .output("")
                    .error(e.getMessage())
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private RunCodeResponse runJavaScriptCodeWithInput(String code, String input, long startTime) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("js_run_");
            Path jsFile = tempDir.resolve("solution.js");
            
            Files.write(jsFile, code.getBytes());
            
            Process runProcess = new ProcessBuilder("node", jsFile.toString())
                    .directory(tempDir.toFile())
                    .redirectErrorStream(false)
                    .start();
            
            // Provide input and close stdin
            try (OutputStream os = runProcess.getOutputStream()) {
                if (input != null && !input.isEmpty()) {
                    os.write((input + "\n").getBytes());
                    os.flush();
                }
            }
            
            if (!runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                return RunCodeResponse.builder()
                        .status("TIMEOUT")
                        .output("")
                        .error("Execution timeout (>" + TIMEOUT_SECONDS + "s)")
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            String output = new String(runProcess.getInputStream().readAllBytes());
            String error = new String(runProcess.getErrorStream().readAllBytes());
            
            if (runProcess.exitValue() != 0) {
                return RunCodeResponse.builder()
                        .status("FAILED")
                        .output(output)
                        .error("Runtime error: " + error)
                        .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                        .build();
            }
            
            return RunCodeResponse.builder()
                    .status("SUCCESS")
                    .output(output)
                    .error(null)
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
                    
        } catch (Exception e) {
            return RunCodeResponse.builder()
                    .status("FAILED")
                    .output("")
                    .error(e.getMessage())
                    .executionTime(formatExecutionTime(System.currentTimeMillis() - startTime))
                    .build();
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }
    
    private String formatExecutionTime(long milliseconds) {
        return milliseconds + "ms";
    }
}
