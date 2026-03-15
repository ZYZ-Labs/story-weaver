const axios = require('axios');

const API_BASE_URL = 'http://localhost:8080/api';

async function testConnection() {
  console.log('=== Story Weaver 前后端连接测试 ===\n');
  
  try {
    // 测试 1: 检查后端是否运行
    console.log('1. 检查后端服务...');
    try {
      const healthResponse = await axios.get(`${API_BASE_URL}/actuator/health`, { timeout: 5000 });
      console.log('   ✅ 后端服务运行正常');
      console.log(`   状态: ${healthResponse.data.status}`);
    } catch (error) {
      console.log('   ❌ 后端服务未运行或无法访问');
      console.log(`   错误: ${error.message}`);
      console.log('   请确保已启动后端服务:');
      console.log('   cd backend && mvn spring-boot:run');
      return;
    }
    
    // 测试 2: 测试登录接口
    console.log('\n2. 测试登录接口...');
    try {
      const loginResponse = await axios.post(`${API_BASE_URL}/auth/login`, {
        username: 'admin',
        password: 'admin123'
      }, { timeout: 5000 });
      
      if (loginResponse.data.code === 200) {
        console.log('   ✅ 登录成功');
        console.log(`   用户: ${loginResponse.data.data.user.username}`);
        console.log(`   Token: ${loginResponse.data.data.token.substring(0, 20)}...`);
        
        const token = loginResponse.data.data.token;
        
        // 测试 3: 测试项目列表接口
        console.log('\n3. 测试项目列表接口...');
        try {
          const projectsResponse = await axios.get(`${API_BASE_URL}/projects`, {
            headers: { Authorization: `Bearer ${token}` },
            timeout: 5000
          });
          
          if (projectsResponse.data.code === 200) {
            console.log('   ✅ 项目列表获取成功');
            console.log(`   项目数量: ${projectsResponse.data.data.length}`);
            
            if (projectsResponse.data.data.length > 0) {
              console.log('   示例项目:');
              projectsResponse.data.data.slice(0, 3).forEach((project, index) => {
                console.log(`   ${index + 1}. ${project.name} (${project.genre || '未分类'})`);
              });
            }
          } else {
            console.log('   ⚠️ 项目列表获取失败');
            console.log(`   错误: ${projectsResponse.data.message}`);
          }
        } catch (projectError) {
          console.log('   ❌ 项目列表接口错误');
          console.log(`   错误: ${projectError.message}`);
        }
      } else {
        console.log('   ⚠️ 登录失败');
        console.log(`   错误: ${loginResponse.data.message}`);
      }
    } catch (loginError) {
      console.log('   ❌ 登录接口错误');
      console.log(`   错误: ${loginError.message}`);
      
      if (loginError.response) {
        console.log(`   状态码: ${loginError.response.status}`);
        console.log(`   响应数据: ${JSON.stringify(loginError.response.data)}`);
      }
    }
    
    // 测试 4: 检查数据库连接
    console.log('\n4. 检查数据库连接...');
    console.log('   请确保 MySQL 服务已启动并执行了初始化脚本:');
    console.log('   mysql -u root -p < backend/src/main/resources/schema.sql');
    
    console.log('\n=== 测试完成 ===');
    console.log('\n下一步:');
    console.log('1. 启动前端: cd front/vuetify-admin && npm run dev');
    console.log('2. 访问 http://localhost:5173');
    console.log('3. 使用 admin/admin123 登录');
    
  } catch (error) {
    console.error('测试过程中发生错误:', error.message);
  }
}

testConnection();