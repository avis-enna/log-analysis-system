import React from 'react';
import { 
  UserIcon, 
  CodeBracketIcon, 
  ShieldCheckIcon,
  BeakerIcon,
  CogIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline';

/**
 * Demo Accounts Page - Shows different user roles and their data scenarios
 */
const DemoAccounts = () => {
  const demoAccounts = [
    {
      username: "admin",
      password: "admin123",
      role: "System Administrator",
      icon: ShieldCheckIcon,
      color: "red",
      description: "Sarah Johnson - Complete system access",
      scenario: "Production Crisis Management",
      features: [
        "🔥 Critical production alerts and system monitoring",
        "💥 Database performance issues and memory warnings", 
        "🚨 Security incidents and failed login attempts",
        "⚠️ Infrastructure alerts (SSL, disk space, network)",
        "📊 Complete access to all system metrics and logs",
        "👥 User management and role administration"
      ],
      sampleLogs: [
        "🔥 CRITICAL: Database connection pool exhausted",
        "💥 SYSTEM ALERT: Memory usage at 95%",
        "🚨 SECURITY: Multiple failed login attempts detected"
      ]
    },
    {
      username: "dev", 
      password: "dev123",
      role: "Lead Developer",
      icon: CodeBracketIcon,
      color: "blue",
      description: "Alex Chen - Development team lead",
      scenario: "Development & Deployment Lifecycle",
      features: [
        "🚀 Deployment logs and build status tracking",
        "🐛 Debug traces and performance profiling",
        "🔧 Code-level error tracking and stack traces",
        "📦 Dependency updates and configuration changes",
        "⚡ Performance metrics and optimization insights",
        "🧪 Integration test results and code coverage"
      ],
      sampleLogs: [
        "🚀 DEPLOYMENT: Starting deployment of v2.1.3",
        "🐛 DEBUG: SQL query execution time: 1247ms",
        "✅ BUILD SUCCESS: Maven build completed"
      ]
    },
    {
      username: "qa",
      password: "qa123", 
      role: "QA Engineer",
      icon: BeakerIcon,
      color: "green",
      description: "Maria Rodriguez - Quality assurance lead",
      scenario: "Testing & Quality Validation",
      features: [
        "🧪 Automated test execution and results",
        "📊 Load testing and performance benchmarks",
        "🔍 API contract validation and regression testing",
        "🛡️ Security testing and penetration test results",
        "📱 Cross-platform and browser compatibility testing",
        "📋 Test coverage reports and quality metrics"
      ],
      sampleLogs: [
        "🧪 TEST START: Automated regression test suite",
        "❌ TEST FAIL: Payment integration timeout",
        "📊 LOAD TEST: Simulating 1000 concurrent users"
      ]
    },
    {
      username: "devops",
      password: "devops123",
      role: "DevOps Engineer", 
      icon: CogIcon,
      color: "purple",
      description: "Jordan Kim - Infrastructure specialist",
      scenario: "Combined Admin + Development Access",
      features: [
        "🔧 Full administrative privileges",
        "🚀 Advanced deployment and infrastructure logs",
        "📊 Complete system and application monitoring",
        "🛠️ Configuration management and automation",
        "⚡ Performance tuning and optimization",
        "🔄 CI/CD pipeline management and monitoring"
      ],
      sampleLogs: [
        "🛠️ CONFIG: Updated connection pool size",
        "🔄 CI/CD: Pipeline deployed successfully",
        "📊 METRICS: Infrastructure health optimal"
      ]
    },
    {
      username: "qaread",
      password: "qaread123",
      role: "QA Analyst",
      icon: UserIcon,
      color: "gray",
      description: "Sam Wilson - Junior QA analyst",
      scenario: "Read-only Testing Overview",
      features: [
        "👀 Read-only access to testing results",
        "📈 Basic performance monitoring dashboard",
        "📋 Test execution summaries and reports",
        "🔍 Limited log search and filtering",
        "📊 Basic analytics and trend viewing",
        "⚠️ Cannot modify or execute tests"
      ],
      sampleLogs: [
        "📋 REPORT: Test execution summary viewed",
        "📈 MONITOR: Performance baseline reviewed",
        "🔍 SEARCH: Test results filtered by status"
      ]
    }
  ];

  const additionalAccounts = [
    {
      username: "john.developer",
      password: "dev456",
      role: "Frontend Developer",
      description: "John Smith - UI/UX specialist"
    },
    {
      username: "lisa.qa", 
      password: "qa456",
      role: "Senior QA",
      description: "Lisa Zhang - Automation specialist"
    }
  ];

  const getColorClasses = (color) => {
    const colors = {
      red: "border-red-200 bg-red-50 dark:bg-red-900/20 dark:border-red-800",
      blue: "border-blue-200 bg-blue-50 dark:bg-blue-900/20 dark:border-blue-800", 
      green: "border-green-200 bg-green-50 dark:bg-green-900/20 dark:border-green-800",
      purple: "border-purple-200 bg-purple-50 dark:bg-purple-900/20 dark:border-purple-800",
      gray: "border-gray-200 bg-gray-50 dark:bg-gray-900/20 dark:border-gray-800"
    };
    return colors[color] || colors.gray;
  };

  const getIconColor = (color) => {
    const colors = {
      red: "text-red-600 dark:text-red-400",
      blue: "text-blue-600 dark:text-blue-400",
      green: "text-green-600 dark:text-green-400", 
      purple: "text-purple-600 dark:text-purple-400",
      gray: "text-gray-600 dark:text-gray-400"
    };
    return colors[color] || colors.gray;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
          🎭 Demo Accounts & Scenarios
        </h1>
        <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <InformationCircleIcon className="h-6 w-6 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="text-sm font-medium text-blue-800 dark:text-blue-200">
                Interactive Demo Environment
              </h3>
              <p className="mt-1 text-sm text-blue-700 dark:text-blue-300">
                Each account has different permissions and sees role-specific log data. 
                Logout and login with different credentials to experience various user scenarios.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Demo Accounts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {demoAccounts.map((account, index) => {
          const Icon = account.icon;
          return (
            <div 
              key={index}
              className={`border rounded-lg p-6 ${getColorClasses(account.color)}`}
            >
              {/* Header */}
              <div className="flex items-start space-x-4 mb-4">
                <div className={`p-3 rounded-lg bg-white dark:bg-gray-800 shadow-sm`}>
                  <Icon className={`h-8 w-8 ${getIconColor(account.color)}`} />
                </div>
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                    {account.role}
                  </h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    {account.description}
                  </p>
                  <div className="mt-2 flex items-center space-x-4 text-sm">
                    <span className="font-mono bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                      {account.username}
                    </span>
                    <span className="font-mono bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                      {account.password}
                    </span>
                  </div>
                </div>
              </div>

              {/* Scenario */}
              <div className="mb-4">
                <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">
                  📊 Data Scenario: {account.scenario}
                </h4>
                <div className="space-y-2">
                  {account.sampleLogs.map((log, logIndex) => (
                    <div key={logIndex} className="text-xs font-mono bg-gray-800 text-green-400 p-2 rounded">
                      {log}
                    </div>
                  ))}
                </div>
              </div>

              {/* Features */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">
                  ✨ What you'll see:
                </h4>
                <ul className="space-y-1">
                  {account.features.map((feature, featureIndex) => (
                    <li key={featureIndex} className="text-xs text-gray-700 dark:text-gray-300">
                      {feature}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          );
        })}
      </div>

      {/* Additional Demo Accounts */}
      <div className="border-t border-gray-200 dark:border-gray-700 pt-8">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
          Additional Demo Accounts
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {additionalAccounts.map((account, index) => (
            <div key={index} className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-medium text-gray-900 dark:text-white">{account.role}</h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">{account.description}</p>
                </div>
                <div className="text-right">
                  <div className="text-sm font-mono text-gray-900 dark:text-white">{account.username}</div>
                  <div className="text-sm font-mono text-gray-600 dark:text-gray-400">{account.password}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Usage Instructions */}
      <div className="mt-8 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          🚀 How to Test Different Scenarios
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 className="font-medium text-gray-900 dark:text-white mb-2">Step-by-Step Testing:</h3>
            <ol className="space-y-2 text-sm text-gray-700 dark:text-gray-300">
              <li>1. 🚪 Click the logout button (top-right corner)</li>
              <li>2. 👤 Login with any demo account credentials</li>
              <li>3. 🔍 Navigate to Dashboard to see role-specific data</li>
              <li>4. 📊 Check Analytics for different insights</li>
              <li>5. 🔄 Try different accounts to compare experiences</li>
            </ol>
          </div>
          <div>
            <h3 className="font-medium text-gray-900 dark:text-white mb-2">What to Look For:</h3>
            <ul className="space-y-2 text-sm text-gray-700 dark:text-gray-300">
              <li>• 🎯 Different log types based on role</li>
              <li>• 📈 Varying dashboard metrics and alerts</li>
              <li>• 🔐 Role-based UI elements and permissions</li>
              <li>• 📋 Unique data scenarios for each user type</li>
              <li>• 🛡️ Access restrictions for limited roles</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DemoAccounts;
