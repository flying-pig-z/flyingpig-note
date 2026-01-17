// 自定义提示框工具类
class DialogUtils {
    // 检查SweetAlert2是否可用
    static isSwalAvailable() {
        return typeof Swal !== 'undefined';
    }

    // 成功提示
    static success(message, title = '成功') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'success',
            title: title,
            text: message,
            confirmButtonText: '确定',
            timer: 3000,
            timerProgressBar: true
        });
    }

    // 错误提示
    static error(message, title = '错误') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'error',
            title: title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    // 警告提示
    static warning(message, title = '警告') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'warning',
            title: title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    // 信息提示
    static info(message, title = '提示') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'info',
            title: title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    // 确认对话框
    static confirm(message, title = '确认') {
        if (!this.isSwalAvailable()) {
            const result = confirm(`${title}: ${message}`);
            return Promise.resolve({ isConfirmed: result });
        }
        return Swal.fire({
            icon: 'question',
            title: title,
            text: message,
            showCancelButton: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            reverseButtons: true
        });
    }

    // 加载提示
    static loading(message = '处理中...') {
        if (!this.isSwalAvailable()) {
            console.log(message);
            return;
        }
        Swal.fire({
            title: message,
            allowOutsideClick: false,
            allowEscapeKey: false,
            allowEnterKey: false,
            showConfirmButton: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });
    }

    // 关闭加载提示
    static closeLoading() {
        if (!this.isSwalAvailable()) {
            console.log('加载完成');
            return;
        }
        Swal.close();
    }
}

// API客户端类
class ApiClient {
    constructor(baseURL = '/api') {
        this.baseURL = baseURL;
        this.token = localStorage.getItem('authToken') || '';
    }

    getHeaders(includeContentType = true) {
        const headers = {};

        if (includeContentType) {
            headers['Content-Type'] = 'application/json';
        }

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }

    async request(url, options = {}) {
        const fullUrl = `${this.baseURL}${url}`;

        const config = {
            ...options,
            headers: {
                ...this.getHeaders(!options.isFormData),
                ...options.headers
            }
        };

        try {
            const response = await fetch(fullUrl, config);

            // 如果token过期或无效，跳转到登录页
            if (response.status === 401) {
                this.redirectToLogin();
                throw new Error('认证失败，请重新登录');
            }

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

    redirectToLogin() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userInfo');
        window.location.href = 'index.html';
    }

    async get(url, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const fullUrl = queryString ? `${url}?${queryString}` : url;
        return this.request(fullUrl);
    }

    async post(url, data, isFormData = false) {
        const options = {
            method: 'POST',
            isFormData: isFormData
        };

        if (isFormData) {
            options.body = data;
        } else {
            options.body = JSON.stringify(data);
        }

        return this.request(url, options);
    }

    async put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async delete(url) {
        return this.request(url, {
            method: 'DELETE'
        });
    }
}

// 创建API客户端实例
const apiClient = new ApiClient();

// 认证API
const authAPI = {
    async logout() {
        try {
            await apiClient.post('/auth/logout');
        } finally {
            localStorage.removeItem('authToken');
            localStorage.removeItem('userInfo');
            window.location.href = 'index.html';
        }
    },

    async getCurrentUser() {
        const result = await apiClient.get('/auth/current');
        return result.data;
    }
};

// 知识库API
const knowledgeBaseAPI = {
    async getList() {
        const result = await apiClient.get('/knowledge-bases');
        return result.data;
    },

    async search(keyword) {
        const result = await apiClient.get('/knowledge-bases/search', { keyword });
        return result.data;
    }
};

// RAG API
const ragAPI = {
    async updateIndex(knowledgeBaseId) {
        const result = await apiClient.post('/rag/updateIndex', {
            knowledgeBaseId
        });
        return result.data;
    },
    
    async forceUpdateIndex(knowledgeBaseId) {
        const result = await apiClient.post('/rag/forceUpdateIndex', {
            knowledgeBaseId
        });
        return result.data;
    }
};

// 认证守卫
function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

// 页面状态
let knowledgeBases = [];

// DOM元素引用
const elements = {
    backToChatBtn: document.getElementById('backToChatBtn'),
    refreshKbBtn: document.getElementById('refreshKbBtn'),
    kbSearch: document.getElementById('kbSearch'),
    knowledgeBases: document.getElementById('knowledgeBases')
};

// 初始化页面
async function initPage() {
    if (!checkAuth()) return;

    try {
        // 加载知识库列表
        await loadKnowledgeBases();

        // 绑定事件监听器
        bindEventListeners();

    } catch (error) {
        console.error('页面初始化失败:', error);
        if (error.message.includes('认证失败')) {
            return; // 已经跳转到登录页
        }
        await DialogUtils.error('页面加载失败: ' + error.message);
    }
}

// 绑定事件监听器
function bindEventListeners() {
    // 返回聊天按钮事件
    elements.backToChatBtn.addEventListener('click', () => {
        window.location.href = 'chat.html';
    });

    // 刷新知识库按钮事件
    elements.refreshKbBtn.addEventListener('click', async () => {
        await loadKnowledgeBases();
    });

    // 知识库搜索事件
    elements.kbSearch.addEventListener('input', () => {
        renderKnowledgeBases(elements.kbSearch.value);
    });
}

// 加载知识库列表
async function loadKnowledgeBases() {
    try {
        showLoading(true);
        knowledgeBases = await knowledgeBaseAPI.getList();
        renderKnowledgeBases();
    } catch (error) {
        console.error('加载知识库失败:', error);
        await DialogUtils.error('加载知识库失败: ' + error.message);
    } finally {
        showLoading(false);
    }
}

// 显示加载状态
function showLoading(show) {
    if (show) {
        elements.knowledgeBases.innerHTML = `
            <div style="text-align: center; padding: 50px; color: #666;">
                <i class="fas fa-spinner fa-spin fa-2x"></i>
                <p style="margin-top: 15px;">加载中...</p>
            </div>
        `;
    }
}

// 渲染知识库列表
function renderKnowledgeBases(searchTerm = '') {
    let filteredKbs = knowledgeBases;

    // 确保searchTerm是字符串类型
    const keyword = String(searchTerm || '').trim();

    if (keyword) {
        filteredKbs = knowledgeBases.filter(kb =>
            (kb.title || '').toLowerCase().includes(keyword.toLowerCase()) ||
            (kb.description || '').toLowerCase().includes(keyword.toLowerCase())
        );
    }

    if (filteredKbs.length === 0) {
        elements.knowledgeBases.innerHTML = `
            <div style="text-align: center; padding: 50px; color: #666;">
                <i class="fas fa-folder-open fa-3x" style="margin-bottom: 20px;"></i>
                <h3>${keyword ? '未找到匹配的知识库' : '暂无知识库'}</h3>
                <p>${keyword ? '尝试使用其他关键词搜索' : '请先创建知识库'}</p>
            </div>
        `;
        return;
    }

    elements.knowledgeBases.innerHTML = filteredKbs.map(kb => `
        <div class="kb-card fade-in">
            <h3>${kb.title || '未命名知识库'}</h3>
            <p>${kb.description || '暂无描述'}</p>
            <div class="info">
                <div class="info-item">
                    <span class="info-label">笔记数量:</span>
                    <span class="info-value">${kb.noteCount || 0} 篇</span>
                </div>
                <div class="info-item">
                    <span class="info-label">索引更新时间:</span>
                    <span class="info-value" id="indexTime-${kb.id}">${formatDateTime(kb.indexUpdateTime) || '从未更新'}</span>
                </div>
            </div>
            <div class="actions">
                <button class="btn btn-primary update-index-btn" data-id="${kb.id}">
                    <i class="fas fa-sync"></i>
                    增量更新
                </button>
                <button class="btn btn-danger force-update-index-btn" data-id="${kb.id}">
                    <i class="fas fa-redo"></i>
                    强制更新
                </button>
            </div>
        </div>
    `).join('');

    // 添加按钮事件监听器
    document.querySelectorAll('.update-index-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const kbId = parseInt(e.target.closest('.update-index-btn').getAttribute('data-id'));
            await updateIndex(kbId, false);
        });
    });

    document.querySelectorAll('.force-update-index-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const kbId = parseInt(e.target.closest('.force-update-index-btn').getAttribute('data-id'));
            await updateIndex(kbId, true);
        });
    });
}

// 格式化日期时间
function formatDateTime(dateStr) {
    if (!dateStr) return '从未更新';
    
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    if (days === 0) return `今天 ${hours}:${minutes}`;
    if (days === 1) return `昨天 ${hours}:${minutes}`;
    if (days < 7) return `${days}天前 ${hours}:${minutes}`;
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

// 更新索引
async function updateIndex(kbId, isForce) {
    try {
        // 找到对应的知识库
        const kb = knowledgeBases.find(k => k.id === kbId);
        if (!kb) {
            throw new Error('未找到知识库');
        }
        
        const actionText = isForce ? '强制更新' : '增量更新';
        const confirmResult = await DialogUtils.confirm(
            `确定要对知识库 "${kb.title || '未命名知识库'}" 进行${actionText}吗？`,
            `${actionText}确认`
        );
        
        if (!confirmResult.isConfirmed) return;
        
        // 禁用按钮
        const updateBtn = document.querySelector(`.update-index-btn[data-id="${kbId}"]`);
        const forceUpdateBtn = document.querySelector(`.force-update-index-btn[data-id="${kbId}"]`);
        
        if (updateBtn) {
            updateBtn.disabled = true;
            updateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 更新中...';
        }
        
        if (forceUpdateBtn) {
            forceUpdateBtn.disabled = true;
            forceUpdateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 更新中...';
        }
        
        DialogUtils.loading(`${actionText}中...`);
        
        // 调用API
        let result;
        if (isForce) {
            result = await ragAPI.forceUpdateIndex(kbId);
        } else {
            result = await ragAPI.updateIndex(kbId);
        }
        
        DialogUtils.closeLoading();
        
        // 更新成功提示
        let message = `${actionText}完成!\n`;
        message += `新增: ${result.insertedCount} 篇\n`;
        message += `更新: ${result.updatedCount} 篇\n`;
        message += `跳过: ${result.skippedCount} 篇`;
        
        if (isForce) {
            message = `${actionText}完成!\n重建: ${result.insertedCount} 篇索引`;
        }
        
        await DialogUtils.success(message, '更新成功');
        
        // 更新索引时间显示
        const indexTimeElement = document.getElementById(`indexTime-${kbId}`);
        if (indexTimeElement) {
            // 重新加载知识库列表以获取最新的索引时间
            await loadKnowledgeBases();
        }
    } catch (error) {
        console.error('更新索引失败:', error);
        DialogUtils.closeLoading();
        await DialogUtils.error('更新失败: ' + error.message, '更新失败');
    } finally {
        // 重新启用按钮
        const updateBtn = document.querySelector(`.update-index-btn[data-id="${kbId}"]`);
        const forceUpdateBtn = document.querySelector(`.force-update-index-btn[data-id="${kbId}"]`);
        
        if (updateBtn) {
            updateBtn.disabled = false;
            updateBtn.innerHTML = '<i class="fas fa-sync"></i> 增量更新';
        }
        
        if (forceUpdateBtn) {
            forceUpdateBtn.disabled = false;
            forceUpdateBtn.innerHTML = '<i class="fas fa-redo"></i> 强制更新';
        }
    }
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', initPage);