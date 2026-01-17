// API客户端类
class ApiClient {
    constructor(baseURL = '/api') {
        this.baseURL = baseURL;
    }

    async request(url, options = {}) {
        const fullUrl = `${this.baseURL}${url}`;

        try {
            const response = await fetch(fullUrl, {
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                },
                ...options
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || '请求失败');
            }

            return data;
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    }

    async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
}

// 创建API客户端实例
const apiClient = new ApiClient();

// 认证API
const authAPI = {
    async login(username, password) {
        const result = await apiClient.post('/auth/login', {
            username,
            password
        });

        if (result.code === 200) {
            return result.data;
        } else {
            throw new Error(result.message);
        }
    },
    
    async register(username, password) {
        const result = await apiClient.post('/auth/register', {
            username,
            password
        });

        if (result.code === 200) {
            return result.data;
        } else {
            throw new Error(result.message);
        }
    }
};

// 检查是否已登录
function checkExistingAuth() {
    const token = localStorage.getItem('authToken');
    if (token) {
        // 已有token，跳转到主页
        window.location.href = 'dashboard.html';
    }
}

// 页面加载时检查
document.addEventListener('DOMContentLoaded', () => {
    checkExistingAuth();
    
    // 设置标签页切换
    setTimeout(setupTabs, 100);
});

// 设置标签页切换功能
function setupTabs() {
    const loginTabBtn = document.querySelector('[data-tab="login"]');
    const registerTabBtn = document.querySelector('[data-tab="register"]');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    
    if (loginTabBtn && registerTabBtn && loginForm && registerForm) {
        loginTabBtn.addEventListener('click', () => {
            loginTabBtn.classList.add('active');
            registerTabBtn.classList.remove('active');
            loginForm.classList.add('active');
            registerForm.classList.remove('active');
        });
        
        registerTabBtn.addEventListener('click', () => {
            registerTabBtn.classList.add('active');
            loginTabBtn.classList.remove('active');
            registerForm.classList.add('active');
            loginForm.classList.remove('active');
        });
    }
}

// 登录处理
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();

    if (!username || !password) {
        alert('请输入用户名和密码');
        return;
    }

    // 显示加载状态
    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 登录中...';
    submitBtn.disabled = true;

    try {
        const loginData = await authAPI.login(username, password);

        // 保存token和用户信息
        localStorage.setItem('authToken', loginData.token);
        localStorage.setItem('userInfo', JSON.stringify(loginData.userInfo));

        // 登录成功，跳转到主页
        window.location.href = 'dashboard.html';

    } catch (error) {
        console.error('登录失败:', error);
        alert('登录失败: ' + error.message);
    } finally {
        // 恢复按钮状态
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
});

// 注册处理
document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('register-username').value.trim();
    const password = document.getElementById('register-password').value.trim();

    if (!username || !password) {
        alert('请输入用户名和密码');
        return;
    }

    // 显示加载状态
    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 注册中...';
    submitBtn.disabled = true;

    try {
        const registerData = await authAPI.register(username, password);

        // 保存token和用户信息
        localStorage.setItem('authToken', registerData.token);
        localStorage.setItem('userInfo', JSON.stringify(registerData.userInfo));

        // 注册成功，跳转到主页
        window.location.href = 'dashboard.html';

    } catch (error) {
        console.error('注册失败:', error);
        alert('注册失败: ' + error.message);
    } finally {
        // 恢复按钮状态
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
});