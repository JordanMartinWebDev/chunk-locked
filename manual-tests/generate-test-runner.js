const fs = require('fs');
const path = require('path');

/**
 * Parse a manual test markdown file and extract test cases
 */
function parseMarkdownTests(content, fileName) {
  const lines = content.split('\n');
  const tests = [];
  let currentTest = null;
  let currentSection = null;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    // Detect test case headers (## Test Case X: Title)
    const testMatch = line.match(/^##\s+Test\s+Case\s+\d+:\s+(.+)$/i);
    if (testMatch) {
      // Save previous test if exists
      if (currentTest) {
        tests.push(currentTest);
      }

      // Start new test
      currentTest = {
        id: `${fileName}-${tests.length + 1}`,
        title: testMatch[1],
        prerequisites: '',
        steps: '',
        expected: '',
        edgeCases: '',
      };
      currentSection = null;
      continue;
    }

    // Skip if no current test
    if (!currentTest) continue;

    // Detect sections
    if (line.startsWith('**Prerequisites**:')) {
      currentSection = 'prerequisites';
      const inline = line.replace(/^\*\*Prerequisites\*\*:\s*/, '');
      if (inline) currentTest.prerequisites = inline;
      continue;
    }
    if (line.startsWith('**Steps**:')) {
      currentSection = 'steps';
      continue;
    }
    if (line.startsWith('**Expected Result**:')) {
      currentSection = 'expected';
      const inline = line.replace(/^\*\*Expected Result\*\*:\s*/, '');
      if (inline) currentTest.expected = inline;
      continue;
    }
    if (line.startsWith('**Edge Cases**:')) {
      currentSection = 'edgeCases';
      const inline = line.replace(/^\*\*Edge Cases\*\*:\s*/, '');
      if (inline) currentTest.edgeCases = inline;
      continue;
    }

    // Add content to current section
    if (currentSection && line) {
      // Remove list markers from steps
      const cleanLine = line.replace(/^\d+\.\s+/, '').replace(/^-\s+/, '');
      if (cleanLine) {
        if (currentTest[currentSection]) {
          currentTest[currentSection] += '\n' + cleanLine;
        } else {
          currentTest[currentSection] = cleanLine;
        }
      }
    }
  }

  // Save last test
  if (currentTest) {
    tests.push(currentTest);
  }

  return tests;
}

/**
 * Generate test suite object from markdown file
 */
function generateTestSuite(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');
  const fileName = path.basename(filePath, '.md');

  // Extract title from first heading
  const titleMatch = content.match(/^#\s+(.+?)(\s+-\s+Manual Test Cases)?$/m);
  const title = titleMatch ? titleMatch[1] : fileName;

  // Parse test cases
  const tests = parseMarkdownTests(content, fileName);

  return {
    id: fileName.toLowerCase().replace(/\s+/g, '-'),
    title: title,
    tests: tests,
  };
}

/**
 * Generate the HTML test runner
 */
function generateHTML(testSuites) {
  const testSuitesJSON = JSON.stringify(testSuites, null, 4);

  return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chunk Locked - Manual Test Runner</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
            overflow: hidden;
        }

        header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }

        header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
        }

        header p {
            font-size: 1.1em;
            opacity: 0.9;
        }

        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
            border-bottom: 3px solid #667eea;
        }

        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            text-align: center;
        }

        .stat-card .label {
            color: #6c757d;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 8px;
        }

        .stat-card .value {
            color: #667eea;
            font-size: 2.5em;
            font-weight: bold;
        }

        .progress-bar {
            background: #e9ecef;
            height: 30px;
            border-radius: 15px;
            overflow: hidden;
            margin: 0 30px 30px 30px;
        }

        .progress-fill {
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            height: 100%;
            transition: width 0.3s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
            font-size: 0.9em;
        }

        .test-suites {
            padding: 30px;
        }

        .test-suite {
            margin-bottom: 30px;
            border: 2px solid #e9ecef;
            border-radius: 8px;
            overflow: hidden;
        }

        .suite-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px 20px;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
            user-select: none;
        }

        .suite-header:hover {
            background: linear-gradient(135deg, #5568d3 0%, #653a8a 100%);
        }

        .suite-header h2 {
            font-size: 1.3em;
        }

        .suite-progress {
            background: rgba(255, 255, 255, 0.3);
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 0.9em;
        }

        .suite-content {
            display: none;
            padding: 20px;
            background: white;
        }

        .suite-content.expanded {
            display: block;
        }

        .test-case {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 6px;
            padding: 20px;
            margin-bottom: 15px;
        }

        .test-header {
            display: flex;
            align-items: center;
            margin-bottom: 15px;
        }

        .test-checkbox {
            width: 24px;
            height: 24px;
            margin-right: 15px;
            cursor: pointer;
            accent-color: #667eea;
        }

        .test-title {
            font-size: 1.2em;
            font-weight: bold;
            color: #333;
        }

        .test-section {
            margin-bottom: 12px;
        }

        .test-section strong {
            color: #667eea;
            display: block;
            margin-bottom: 5px;
        }

        .test-section p {
            color: #495057;
            line-height: 1.6;
            white-space: pre-wrap;
        }

        .test-case.completed {
            opacity: 0.7;
            background: #e7f5e7;
        }

        .test-case.completed .test-title {
            text-decoration: line-through;
            color: #28a745;
        }

        footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #6c757d;
            border-top: 2px solid #e9ecef;
        }

        @media (max-width: 768px) {
            header h1 {
                font-size: 1.8em;
            }

            .stats {
                grid-template-columns: 1fr 1fr;
            }

            .stat-card .value {
                font-size: 2em;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🧪 Chunk Locked Manual Test Runner</h1>
            <p>Track your manual testing progress</p>
        </header>

        <div class="stats">
            <div class="stat-card">
                <div class="label">Total Tests</div>
                <div class="value" id="totalTests">0</div>
            </div>
            <div class="stat-card">
                <div class="label">Completed</div>
                <div class="value" id="completedTests">0</div>
            </div>
            <div class="stat-card">
                <div class="label">Remaining</div>
                <div class="value" id="remainingTests">0</div>
            </div>
            <div class="stat-card">
                <div class="label">Progress</div>
                <div class="value" id="progressPercent">0%</div>
            </div>
        </div>

        <div class="progress-bar">
            <div class="progress-fill" id="progressFill" style="width: 0%">0%</div>
        </div>

        <div class="test-suites" id="testSuites"></div>

        <footer>
            <p><strong>Chunk Locked</strong> - Minecraft Fabric Mod</p>
            <p>Generated from markdown test files</p>
        </footer>
    </div>

    <script>
        // Test data from markdown files
        const testSuites = ${testSuitesJSON};

        // Current state
        const state = {
            completed: new Set(),
            expanded: new Set()
        };

        // Initialize the test runner
        function init() {
            renderTestSuites();
            updateStats();
        }

        // Render all test suites
        function renderTestSuites() {
            const container = document.getElementById('testSuites');
            container.innerHTML = '';

            testSuites.forEach(suite => {
                const suiteElement = createTestSuite(suite);
                container.appendChild(suiteElement);
            });
        }

        // Create a test suite element
        function createTestSuite(suite) {
            const div = document.createElement('div');
            div.className = 'test-suite';

            const completed = suite.tests.filter(t => state.completed.has(t.id)).length;
            const total = suite.tests.length;
            const isExpanded = state.expanded.has(suite.id);

            div.innerHTML = \`
                <div class="suite-header" onclick="toggleSuite('\${suite.id}')">
                    <h2>\${suite.title}</h2>
                    <span class="suite-progress">\${completed}/\${total}</span>
                </div>
                <div class="suite-content \${isExpanded ? 'expanded' : ''}" id="suite-\${suite.id}">
                    \${suite.tests.map(test => createTestCase(test)).join('')}
                </div>
            \`;

            return div;
        }

        // Create a test case HTML
        function createTestCase(test) {
            const isCompleted = state.completed.has(test.id);
            return \`
                <div class="test-case \${isCompleted ? 'completed' : ''}" id="test-\${test.id}">
                    <div class="test-header">
                        <input type="checkbox" 
                               class="test-checkbox" 
                               \${isCompleted ? 'checked' : ''}
                               onchange="toggleTest('\${test.id}')">
                        <span class="test-title">\${test.title}</span>
                    </div>
                    \${test.prerequisites ? \`
                    <div class="test-section">
                        <strong>Prerequisites:</strong>
                        <p>\${escapeHtml(test.prerequisites)}</p>
                    </div>
                    \` : ''}
                    \${test.steps ? \`
                    <div class="test-section">
                        <strong>Steps:</strong>
                        <p>\${escapeHtml(test.steps)}</p>
                    </div>
                    \` : ''}
                    \${test.expected ? \`
                    <div class="test-section">
                        <strong>Expected Result:</strong>
                        <p>\${escapeHtml(test.expected)}</p>
                    </div>
                    \` : ''}
                    \${test.edgeCases ? \`
                    <div class="test-section">
                        <strong>Edge Cases:</strong>
                        <p>\${escapeHtml(test.edgeCases)}</p>
                    </div>
                    \` : ''}
                </div>
            \`;
        }

        // Toggle test suite expansion
        function toggleSuite(suiteId) {
            if (state.expanded.has(suiteId)) {
                state.expanded.delete(suiteId);
            } else {
                state.expanded.add(suiteId);
            }
            
            const content = document.getElementById(\`suite-\${suiteId}\`);
            content.classList.toggle('expanded');
        }

        // Toggle test completion
        function toggleTest(testId) {
            if (state.completed.has(testId)) {
                state.completed.delete(testId);
            } else {
                state.completed.add(testId);
            }
            
            renderTestSuites();
            updateStats();
        }

        // Update statistics
        function updateStats() {
            const total = testSuites.reduce((sum, suite) => sum + suite.tests.length, 0);
            const completed = state.completed.size;
            const remaining = total - completed;
            const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

            document.getElementById('totalTests').textContent = total;
            document.getElementById('completedTests').textContent = completed;
            document.getElementById('remainingTests').textContent = remaining;
            document.getElementById('progressPercent').textContent = percent + '%';

            const progressFill = document.getElementById('progressFill');
            progressFill.style.width = percent + '%';
            progressFill.textContent = percent + '%';
        }

        // Escape HTML to prevent XSS
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        // Initialize on page load
        init();
    </script>
</body>
</html>`;
}

/**
 * Main function
 */
function main() {
  const manualTestsDir = __dirname;
  const outputFile = path.join(manualTestsDir, 'test-runner.html');

  // Find all markdown files in manual-tests directory
  const files = fs
    .readdirSync(manualTestsDir)
    .filter((f) => f.endsWith('.md'))
    .map((f) => path.join(manualTestsDir, f));

  console.log(`Found ${files.length} markdown test files`);

  // Generate test suites from markdown files
  const testSuites = files.map((file) => {
    console.log(`  Parsing: ${path.basename(file)}`);
    return generateTestSuite(file);
  });

  // Generate HTML
  const html = generateHTML(testSuites);

  // Write output
  fs.writeFileSync(outputFile, html, 'utf-8');
  console.log(`\n✅ Generated: ${outputFile}`);
  console.log(`   Test suites: ${testSuites.length}`);
  console.log(
    `   Total tests: ${testSuites.reduce((sum, s) => sum + s.tests.length, 0)}`
  );
}

// Run if executed directly
if (require.main === module) {
  main();
}

module.exports = { parseMarkdownTests, generateTestSuite, generateHTML };
