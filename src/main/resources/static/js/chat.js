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
    async answer(question, knowledgeBaseIds, topK = 5) {
        const result = await apiClient.post('/rag/answer', {
            question,
            knowledgeBaseIds,
            topK
        });
        return result;
    }
};

// 页面状态
let knowledgeBases = [];
let currentUser = null;
let selectedKnowledgeBaseIds = []; // 选中的知识库ID列表

// DOM元素引用
const elements = {
    backToKbBtn: document.getElementById('backToKbBtn'),
    refreshKbBtn: document.getElementById('refreshKbBtn'),
    manageIndexBtn: document.getElementById('manageIndexBtn'),
    kbSearch: document.getElementById('kbSearch'),
    knowledgeBasesList: document.getElementById('knowledgeBases'),
    chatMessages: document.getElementById('chatMessages'),
    messageInput: document.getElementById('messageInput'),
    sendBtn: document.getElementById('sendBtn'),
    selectedKbNames: document.getElementById('selectedKbNames')
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

// 检查认证状态
function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

// 加载用户信息
async function loadUserInfo() {
    try {
        // 优先从localStorage获取
        const userInfo = localStorage.getItem('userInfo');
        if (userInfo) {
            currentUser = JSON.parse(userInfo);
        } else {
            // 从API获取
            currentUser = await authAPI.getCurrentUser();
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
        showKbLoading(true);
        knowledgeBases = await knowledgeBaseAPI.getList();
        renderKnowledgeBases();
    } catch (error) {
        console.error('加载知识库失败:', error);
        await DialogUtils.error('加载知识库失败: ' + error.message);
    } finally {
        showKbLoading(false);
    }
}

// 显示知识库加载状态
function showKbLoading(show) {
    if (show) {
        elements.knowledgeBasesList.innerHTML = `
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
        elements.knowledgeBasesList.innerHTML = `
            <div style="text-align: center; padding: 50px; color: #666;">
                <i class="fas fa-folder-open fa-3x" style="margin-bottom: 20px;"></i>
                <h3>${keyword ? '未找到匹配的知识库' : '暂无知识库'}</h3>
                <p>${keyword ? '尝试使用其他关键词搜索' : '请先创建知识库'}</p>
            </div>
        `;
        return;
    }

    elements.knowledgeBasesList.innerHTML = filteredKbs.map(kb => {
        const isSelected = selectedKnowledgeBaseIds.includes(kb.id);
        return `
            <div class="kb-item ${isSelected ? 'selected' : ''}" data-id="${kb.id}">
                <i class="fas fa-book kb-icon"></i>
                <span class="kb-name">${kb.title || '未命名知识库'}</span>
                ${isSelected ? '<i class="fas fa-check"></i>' : ''}
            </div>
        `;
    }).join('');

    // 添加点击事件
    document.querySelectorAll('.kb-item').forEach(item => {
        item.addEventListener('click', () => {
            const kbId = parseInt(item.getAttribute('data-id'));
            toggleKnowledgeBaseSelection(kbId);
        });
    });
}

// 切换知识库选择状态
function toggleKnowledgeBaseSelection(kbId) {
    const index = selectedKnowledgeBaseIds.indexOf(kbId);
    if (index > -1) {
        // 已选中，取消选择
        selectedKnowledgeBaseIds.splice(index, 1);
    } else {
        // 未选中，添加选择
        selectedKnowledgeBaseIds.push(kbId);
    }

    // 更新UI
    renderKnowledgeBases(elements.kbSearch.value);
    updateSelectedKbInfo();
}

// 更新选中知识库信息显示
function updateSelectedKbInfo() {
    if (selectedKnowledgeBaseIds.length === 0) {
        elements.selectedKbNames.textContent = '全部';
        return;
    }

    const selectedKbs = knowledgeBases.filter(kb => selectedKnowledgeBaseIds.includes(kb.id));
    const names = selectedKbs.map(kb => kb.title || '未命名知识库').join(', ');
    elements.selectedKbNames.textContent = names;
}

// 绑定事件监听器
function bindEventListeners() {
    // 返回知识库按钮事件
    elements.backToKbBtn.addEventListener('click', () => {
        window.location.href = 'dashboard.html';
    });

    // 刷新知识库按钮事件
    elements.refreshKbBtn.addEventListener('click', async () => {
        await loadKnowledgeBases();
    });

    // 管理索引按钮事件
    if (elements.manageIndexBtn) {
        elements.manageIndexBtn.addEventListener('click', () => {
            window.location.href = 'kb-index-management.html';
        });
    }

    // 知识库搜索事件
    elements.kbSearch.addEventListener('input', () => {
        renderKnowledgeBases(elements.kbSearch.value);
    });

    // 发送消息按钮事件
    elements.sendBtn.addEventListener('click', sendMessage);

    // 消息输入框回车事件
    elements.messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
}

// 解析消息中的知识库引用
function parseKnowledgeBaseReferences(message) {
    // 匹配 @知识库名称 的模式（支持中文和英文）
    const regex = /@([^\s@]+(?:\s+[^\s@]+)*)/g;
    const matches = [];
    let match;

    while ((match = regex.exec(message)) !== null) {
        matches.push({
            fullMatch: match[0],
            kbName: match[1].trim(),
            startIndex: match.index,
            endIndex: match.index + match[0].length
        });
    }

    // 如果没有找到@引用，则使用选中的知识库
    if (matches.length === 0) {
        return {
            cleanMessage: message.trim(),
            knowledgeBaseIds: selectedKnowledgeBaseIds.length > 0 ? selectedKnowledgeBaseIds :
                knowledgeBases.map(kb => kb.id) // 如果没有选中任何知识库，则使用全部
        };
    }

    // 查找匹配的知识库
    const referencedKbIds = [];
    let cleanMessage = message;

    // 从后往前替换，避免索引偏移
    for (let i = matches.length - 1; i >= 0; i--) {
        const match = matches[i];
        // 精确匹配知识库名称
        const kb = knowledgeBases.find(k => k.title === match.kbName);

        if (kb) {
            referencedKbIds.push(kb.id);
            // 移除@引用部分
            cleanMessage = cleanMessage.substring(0, match.startIndex) +
                cleanMessage.substring(match.endIndex);
        }
    }

    // 如果找到了@引用但没有匹配到具体的知识库，则使用选中的知识库
    if (referencedKbIds.length === 0) {
        // 移除所有@引用
        const cleanedMsg = message.trim().replace(/@([^\s@]+(?:\s+[^\s@]+)*)/g, '').trim();
        return {
            cleanMessage: cleanedMsg,
            knowledgeBaseIds: selectedKnowledgeBaseIds.length > 0 ? selectedKnowledgeBaseIds :
                knowledgeBases.map(kb => kb.id)
        };
    }

    return {
        cleanMessage: cleanMessage.trim(),
        knowledgeBaseIds: referencedKbIds
    };
}

// 发送消息
async function sendMessage() {
    const message = elements.messageInput.value.trim();
    if (!message) return;

    // 解析消息中的知识库引用
    const { cleanMessage, knowledgeBaseIds } = parseKnowledgeBaseReferences(message);

    if (knowledgeBaseIds.length === 0) {
        await DialogUtils.warning('请至少选择一个知识库');
        return;
    }

    // 添加用户消息到聊天界面
    addMessageToChat(cleanMessage, 'user');

    // 清空输入框
    elements.messageInput.value = '';

    // 禁用发送按钮
    elements.sendBtn.disabled = true;
    elements.sendBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 处理中';

    try {
        // 添加AI思考中的消息
        const aiThinkingMessageElement = addAiThinkingMessage();

        // 调用RAG API
        const response = await ragAPI.answer(cleanMessage, knowledgeBaseIds);
        const answer = response.data;

        // 移除思考中的消息
        aiThinkingMessageElement.remove();

        // 添加AI回复到聊天界面，并附带相关文档信息
        let answerWithReferences = answer.answer;
        if (answer.relevantDocuments && answer.relevantDocuments.length > 0) {
            // 按noteTitle去重
            const uniqueDocTitles = [...new Set(
                answer.relevantDocuments.map(doc => doc.noteTitle || '未命名笔记')
            )];
            answerWithReferences += '\n\n参考文档:\n';
            uniqueDocTitles.forEach((title, index) => {
                answerWithReferences += `${index + 1}. ${title}\n`;
            });
        }
        addMessageToChat(answerWithReferences, 'ai');
    } catch (error) {
        console.error('发送消息失败:', error);
        // 移除思考中的消息
        const thinkingElement = document.getElementById('ai-thinking-message');
        if (thinkingElement) thinkingElement.remove();

        addMessageToChat(`抱歉，处理您的问题时出现错误: ${error.message}`, 'ai');
    } finally {
        // 恢复发送按钮
        elements.sendBtn.disabled = false;
        elements.sendBtn.innerHTML = '<i class="fas fa-paper-plane"></i> 发送';
    }
}

// 添加消息到聊天界面
function addMessageToChat(content, sender) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${sender}-message`;

    const contentElement = document.createElement('div');
    contentElement.className = 'message-content';
    contentElement.innerHTML = formatMessageContent(content);

    const metaElement = document.createElement('div');
    metaElement.className = 'message-meta';
    metaElement.textContent = getCurrentTimeString();

    messageElement.appendChild(contentElement);
    messageElement.appendChild(metaElement);

    elements.chatMessages.appendChild(messageElement);

    // 滚动到底部
    scrollToBottom();
}

// 添加AI思考中的消息
function addAiThinkingMessage() {
    const messageElement = document.createElement('div');
    messageElement.className = 'message ai-message';
    messageElement.id = 'ai-thinking-message';

    const contentElement = document.createElement('div');
    contentElement.className = 'message-content';
    contentElement.innerHTML = `
        <div class="loading-dots">
            <span>●</span>
            <span>●</span>
            <span>●</span>
        </div>
    `;

    messageElement.appendChild(contentElement);
    elements.chatMessages.appendChild(messageElement);

    // 滚动到底部
    scrollToBottom();

    return messageElement;
}

// 添加相关文档信息
function addRelatedDocuments(documents) {
    // 此函数不再使用，因为我们直接将引用附加到答案中
}

// 格式化消息内容
function formatMessageContent(content) {
    // 使用 marked.js 库来解析 Markdown
    if (typeof marked !== 'undefined') {
        try {
            // 配置 marked 选项
            marked.setOptions({
                breaks: true, // 转换 \n 为 <br>
                gfm: true, // GitHub 风格 Markdown
                smartLists: true,
                smartypants: true
            });

            // 解析 Markdown 内容
            return marked.parse(content);
        } catch (e) {
            console.warn('Markdown解析失败，使用纯文本显示:', e);
        }
    }

    // 如果没有 marked.js 或解析失败，使用简单的换行处理
    return escapeHtml(content).replace(/\n/g, '<br>');
}

// 转义HTML特殊字符
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };

    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// 获取当前时间字符串
function getCurrentTimeString() {
    const now = new Date();
    return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
}

// 滚动到底部
function scrollToBottom() {
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', initPage);