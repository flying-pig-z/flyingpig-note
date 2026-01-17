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

    // 输入对话框
    static async prompt(message, title = '输入', defaultValue = '', placeholder = '') {
        if (!this.isSwalAvailable()) {
            const result = prompt(`${title}: ${message}`, defaultValue);
            return result || null;
        }
        const result = await Swal.fire({
            title: title,
            text: message,
            input: 'text',
            inputValue: defaultValue,
            inputPlaceholder: placeholder,
            showCancelButton: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputValidator: (value) => {
                if (!value || !value.trim()) {
                    return '请输入内容';
                }
            }
        });

        return result.isConfirmed ? result.value : null;
    }

    // 可选输入对话框（允许空值）
    static async optionalPrompt(message, title = '输入', defaultValue = '', placeholder = '') {
        if (!this.isSwalAvailable()) {
            const result = prompt(`${title}: ${message}`, defaultValue);
            return result || '';
        }
        const result = await Swal.fire({
            title: title,
            text: message,
            input: 'text',
            inputValue: defaultValue,
            inputPlaceholder: placeholder,
            showCancelButton: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消'
        });

        return result.isConfirmed ? (result.value || '') : null;
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

    // 危险确认对话框
    static dangerConfirm(message, title = '危险操作') {
        if (!this.isSwalAvailable()) {
            const result = confirm(`${title}: ${message}`);
            return Promise.resolve({ isConfirmed: result });
        }
        return Swal.fire({
            icon: 'warning',
            title: title,
            html: message,
            showCancelButton: true,
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            confirmButtonColor: '#dc3545',
            reverseButtons: true,
            focusCancel: true
        });
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
    },

    async create(title, description) {
        const result = await apiClient.post('/knowledge-bases', {
            title,
            description
        });
        return result.data;
    },

    async update(id, title, description) {
        const result = await apiClient.put(`/knowledge-bases/${id}`, {
            title,
            description
        });
        return result.data;
    },

    async delete(id) {
        await apiClient.delete(`/knowledge-bases/${id}`);
    }
};

// 批量导入API
const batchImportAPI = {
    async importFolder(files, knowledgeBaseId) {
        const formData = new FormData();
        formData.append('knowledgeBaseId', knowledgeBaseId);
        
        for (let i = 0; i < files.length; i++) {
            formData.append('files', files[i]);
        }

        return await apiClient.post('/batch-import/folder', formData, true);
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
let currentUser = null;

// DOM元素引用
const elements = {
    knowledgeBases: document.getElementById('knowledgeBases'),
    kbSearch: document.getElementById('kbSearch'),
    addKbBtn: document.getElementById('addKbBtn'),
    importKbBtn: document.getElementById('importKbBtn'), // 新增的导入按钮
    logoutBtn: document.getElementById('logoutBtn'),
    currentUser: document.getElementById('currentUser'),
    // 模态框相关元素
    folderModal: document.getElementById('folderModal'),
    folderInput: document.getElementById('folderInput'),
    knowledgeBaseName: document.getElementById('knowledgeBaseName'),
    knowledgeBaseDesc: document.getElementById('knowledgeBaseDesc'),
    cancelImport: document.getElementById('cancelImport'),
    confirmImport: document.getElementById('confirmImport'),
    closeModal: document.querySelector('.close')
};

// 初始化页面
async function initPage() {
    if (!checkAuth()) return;

    try {
        // 加载用户信息
        await loadUserInfo();

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
    // 搜索框事件
    elements.kbSearch.addEventListener('input', () => {
        renderKnowledgeBases(elements.kbSearch.value);
    });

    // 新建知识库按钮事件
    elements.addKbBtn.addEventListener('click', async () => {
        const title = await DialogUtils.prompt('请输入知识库名称', '新建知识库', '', '例如：学习笔记');
        if (!title) return;

        const description = await DialogUtils.optionalPrompt('请输入知识库描述（可选）', '知识库描述', '', '例如：用于记录日常学习内容');
        if (description === null) return;

        try {
            elements.addKbBtn.disabled = true;
            elements.addKbBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 创建中...';

            const newKb = await knowledgeBaseAPI.create(title.trim(), description.trim());
            knowledgeBases.unshift(newKb); // 添加到列表开头
            renderKnowledgeBases();

            await DialogUtils.success('知识库创建成功！', '创建成功');
        } catch (error) {
            console.error('创建知识库失败:', error);
            await DialogUtils.error('创建失败: ' + error.message, '创建失败');
        } finally {
            elements.addKbBtn.disabled = false;
            elements.addKbBtn.innerHTML = '<i class="fas fa-plus"></i> 新建知识库';
        }
    });

    // 批量导入按钮事件
    elements.importKbBtn.addEventListener('click', () => {
        // 清空之前的选择
        elements.folderInput.value = '';
        elements.knowledgeBaseName.value = '';
        elements.knowledgeBaseDesc.value = '';
        // 显示模态框
        elements.folderModal.style.display = 'flex';
    });

    // 模态框关闭事件
    elements.closeModal.addEventListener('click', () => {
        elements.folderModal.style.display = 'none';
    });

    elements.cancelImport.addEventListener('click', () => {
        elements.folderModal.style.display = 'none';
    });

    // 点击模态框外部关闭
    window.addEventListener('click', (event) => {
        if (event.target === elements.folderModal) {
            elements.folderModal.style.display = 'none';
        }
    });

    // 确认导入事件
    elements.confirmImport.addEventListener('click', handleBatchImport);

    // 退出登录按钮事件
    elements.logoutBtn.addEventListener('click', async () => {
        const result = await DialogUtils.confirm('确定要退出登录吗？', '退出确认');

        if (result.isConfirmed) {
            try {
                DialogUtils.loading('正在退出...');
                await authAPI.logout();
            } catch (error) {
                console.error('登出失败:', error);
                // 即使登出失败也清除本地数据
                localStorage.removeItem('authToken');
                localStorage.removeItem('userInfo');
                window.location.href = 'index.html';
            }
        }
    });
    
    // 添加聊天页面入口按钮
    const chatBtn = document.createElement('button');
    chatBtn.id = 'chatBtn';
    chatBtn.className = 'chat-btn';
    chatBtn.innerHTML = '<i class="fas fa-comments"></i> 智能问答';
    chatBtn.addEventListener('click', () => {
        window.location.href = 'chat.html';
    });
    
    // 将聊天按钮插入到搜索区域
    document.querySelector('.search-section').appendChild(chatBtn);
}

// 处理批量导入
async function handleBatchImport() {
    const files = elements.folderInput.files;
    const kbName = elements.knowledgeBaseName.value.trim();
    const kbDesc = elements.knowledgeBaseDesc.value.trim();

    // 验证输入
    if (files.length === 0) {
        await DialogUtils.warning('请选择一个包含Markdown文件的文件夹');
        return;
    }

    if (!kbName) {
        await DialogUtils.warning('请输入知识库名称');
        return;
    }

    // 筛选出Markdown文件
    const mdFiles = Array.from(files).filter(file => 
        file.name.toLowerCase().endsWith('.md') || 
        file.name.toLowerCase().endsWith('.markdown')
    );

    if (mdFiles.length === 0) {
        await DialogUtils.warning('所选文件夹中没有找到Markdown文件(.md或.markdown)');
        return;
    }

    try {
        // 创建知识库
        DialogUtils.loading('正在创建知识库...');
        const newKb = await knowledgeBaseAPI.create(kbName, kbDesc);
        knowledgeBases.unshift(newKb);
        renderKnowledgeBases();

        // 导入文件
        DialogUtils.closeLoading();
        DialogUtils.loading(`正在导入${mdFiles.length}个Markdown文件...`);

        // 调用后端API导入文件
        const importResult = await batchImportAPI.importFolder(mdFiles, newKb.id);

        DialogUtils.closeLoading();
        
        // 显示导入结果
        let resultMessage = `成功导入 ${importResult.data.importedCount} 个文件`;
        if (importResult.data.failedCount > 0) {
            resultMessage += `，失败 ${importResult.data.failedCount} 个文件`;
        }
        
        await DialogUtils.success(resultMessage, '导入完成');
        
        // 关闭模态框
        elements.folderModal.style.display = 'none';
        
        // 刷新知识库列表
        await loadKnowledgeBases();
    } catch (error) {
        console.error('批量导入失败:', error);
        DialogUtils.closeLoading();
        await DialogUtils.error('导入失败: ' + error.message, '导入失败');
    }
}

// 加载用户信息
async function loadUserInfo() {
    try {
        // 优先从localStorage获取
        const userInfo = localStorage.getItem('userInfo');
        if (userInfo) {
            currentUser = JSON.parse(userInfo);
            elements.currentUser.textContent = `欢迎，${currentUser.username}`;
        } else {
            // 从API获取
            currentUser = await authAPI.getCurrentUser();
            elements.currentUser.textContent = `欢迎，${currentUser.username}`;
            localStorage.setItem('userInfo', JSON.stringify(currentUser));
        }
    } catch (error) {
        console.error('加载用户信息失败:', error);
        throw error;
    }
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
                <p>${keyword ? '尝试使用其他关键词搜索' : '点击"新建知识库"创建您的第一个知识库'}</p>
            </div>
        `;
        return;
    }

    elements.knowledgeBases.innerHTML = filteredKbs.map(kb => `
        <div class="kb-card fade-in" onclick="openKnowledgeBase(${kb.id}, '${(kb.title || '').replace(/'/g, "\\'")}')">
            <h3>${kb.title || '未命名知识库'}</h3>
            <p>${kb.description || '暂无描述'}</p>
            <div class="stats">
                <span><i class="fas fa-file-alt"></i> ${kb.noteCount || 0} 篇笔记</span>
                <span><i class="fas fa-clock"></i> ${formatDate(kb.updateTime)}</span>
            </div>
            <div class="kb-actions" style="margin-top: 15px; opacity: 0; transition: opacity 0.3s;">
                <button onclick="event.stopPropagation(); editKnowledgeBase(${kb.id}, '${(kb.title || '').replace(/'/g, "\\'")}', '${(kb.description || '').replace(/'/g, "\\'")}')" 
                        style="background: #007bff; color: white; border: none; padding: 5px 10px; border-radius: 4px; margin-right: 8px; cursor: pointer;">
                    <i class="fas fa-edit"></i> 编辑
                </button>
                <button onclick="event.stopPropagation(); deleteKnowledgeBase(${kb.id}, '${(kb.title || '未命名知识库').replace(/'/g, "\\'")}')" 
                        style="background: #dc3545; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer;">
                    <i class="fas fa-trash"></i> 删除
                </button>
            </div>
        </div>
    `).join('');

    // 添加悬停效果
    document.querySelectorAll('.kb-card').forEach(card => {
        card.addEventListener('mouseenter', () => {
            const actions = card.querySelector('.kb-actions');
            if (actions) actions.style.opacity = '1';
        });
        card.addEventListener('mouseleave', () => {
            const actions = card.querySelector('.kb-actions');
            if (actions) actions.style.opacity = '0';
        });
    });
}

// 格式化日期
function formatDate(dateStr) {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) return '今天';
    if (days === 1) return '昨天';
    if (days < 7) return `${days}天前`;

    return date.toLocaleDateString();
}

// 编辑知识库
async function editKnowledgeBase(id, currentTitle, currentDescription) {
    const newTitle = await DialogUtils.prompt('请输入新的知识库名称', '编辑知识库', currentTitle);
    if (!newTitle) return;

    const newDescription = await DialogUtils.optionalPrompt('请输入新的知识库描述', '编辑描述', currentDescription);
    if (newDescription === null) return;

    try {
        const updatedKb = await knowledgeBaseAPI.update(id, newTitle.trim(), newDescription.trim());

        // 更新本地数据
        const index = knowledgeBases.findIndex(kb => kb.id === id);
        if (index !== -1) {
            knowledgeBases[index] = updatedKb;
            renderKnowledgeBases();
        }

        await DialogUtils.success('知识库更新成功！', '更新成功');
    } catch (error) {
        console.error('更新知识库失败:', error);
        await DialogUtils.error('更新失败: ' + error.message, '更新失败');
    }
}

// 删除知识库
async function deleteKnowledgeBase(id, title) {
    const result = await DialogUtils.dangerConfirm(
        `确定要删除知识库 <strong>"${title}"</strong> 吗？<br><br><span style="color: #dc3545;">⚠️ 注意：删除知识库将同时删除其中的所有笔记，此操作不可撤销！</span>`,
        '删除知识库'
    );

    if (!result.isConfirmed) return;

    try {
        DialogUtils.loading('正在删除...');
        await knowledgeBaseAPI.delete(id);

        // 从本地数据中移除
        knowledgeBases = knowledgeBases.filter(kb => kb.id !== id);
        renderKnowledgeBases();

        DialogUtils.closeLoading();
        await DialogUtils.success('知识库删除成功！', '删除成功');
    } catch (error) {
        console.error('删除知识库失败:', error);
        DialogUtils.closeLoading();
        await DialogUtils.error('删除失败: ' + error.message, '删除失败');
    }
}

// 打开知识库
function openKnowledgeBase(kbId, kbTitle) {
    const encodedTitle = encodeURIComponent(kbTitle);
    window.location.href = `notes.html?kbId=${kbId}&kbTitle=${encodedTitle}`;
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', initPage);