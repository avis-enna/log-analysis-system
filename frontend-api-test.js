// Simple test to verify frontend API connectivity
console.log('🔍 Testing Frontend-Backend Connection...');

// Test direct API call from frontend context
fetch('/api/v1/dashboard/stats')
  .then(response => {
    console.log('📡 API Response Status:', response.status);
    return response.json();
  })
  .then(data => {
    console.log('✅ Dashboard Stats Data:', data);
    console.log('📊 Total Logs:', data.totalLogs);
    console.log('🏥 System Health:', data.systemHealth);
  })
  .catch(error => {
    console.error('❌ API Error:', error);
  });

// Test logs endpoint
fetch('/api/v1/logs?page=0&size=5')
  .then(response => response.json())
  .then(data => {
    console.log('✅ Logs Data:', data);
    console.log('📈 Total Elements:', data.totalElements);
    console.log('📝 First Log:', data.logs[0]);
  })
  .catch(error => {
    console.error('❌ Logs API Error:', error);
  });

console.log('🔬 Test completed - check console for results');
