const axios = require('axios');

// Test API from frontend perspective
const API_BASE_URL = 'http://localhost:3002/api/v1';

async function testAPI() {
  try {
    console.log('Testing API connection...');
    
    // Test dashboard stats
    const response = await axios.get(`${API_BASE_URL}/dashboard/stats`);
    console.log('✅ Dashboard stats:', response.data);
    
    // Test logs
    const logsResponse = await axios.get(`${API_BASE_URL}/logs?page=0&size=5`);
    console.log('✅ Logs count:', logsResponse.data.totalElements);
    
    console.log('🎉 All API tests passed!');
  } catch (error) {
    console.error('❌ API test failed:', error.message);
    if (error.response) {
      console.error('Response status:', error.response.status);
      console.error('Response data:', error.response.data);
    }
  }
}

testAPI();
