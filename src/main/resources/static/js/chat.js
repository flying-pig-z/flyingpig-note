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
    async answer(question, knowledgeBaseIds, history = []) {
        const result = await apiClient.post('/rag/answer', {
            question,
            knowledgeBaseIds,
            history
        });
        return result;
    },

    async answerStream(question, knowledgeBaseIds, history = [], onDelta) {
        const response = await fetch(`${apiClient.baseURL}/rag/answer/stream`, {
            method: 'POST',
            headers: apiClient.getHeaders(),
            body: JSON.stringify({
                question,
                knowledgeBaseIds,
                history
            })
        });

        if (response.status === 401) {
            apiClient.redirectToLogin();
            throw new Error('认证失败，请重新登录');
        }

        if (!response.ok) {
            let message = '请求失败';
            try {
                const errorBody = await response.json();
                message = errorBody.message || message;
            } catch (_) {
                const errorText = await response.text();
                if (errorText) {
                    message = errorText;
                }
            }
            throw new Error(message);
        }

        if (!response.body) {
            throw new Error('浏览器不支持流式响应');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';
        let finalPayload = null;
        const consumeBufferedEvents = () => {
            let delimiterIndex;
            while ((delimiterIndex = buffer.indexOf('\n\n')) !== -1) {
                const rawEvent = buffer.slice(0, delimiterIndex).trim();
                buffer = buffer.slice(delimiterIndex + 2);

                if (!rawEvent) {
                    continue;
                }

                const parsedEvent = parseSseEvent(rawEvent);
                if (!parsedEvent) {
                    continue;
                }

                if (parsedEvent.event === 'delta') {
                    const chunk = parsedEvent.data?.content || '';
                    if (chunk && typeof onDelta === 'function') {
                        onDelta(chunk);
                    }
                    continue;
                }

                if (parsedEvent.event === 'done') {
                    finalPayload = parsedEvent.data;
                    continue;
                }

                if (parsedEvent.event === 'error') {
                    throw new Error(parsedEvent.data?.message || '流式响应失败');
                }
            }
        };

        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            }

            buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
            consumeBufferedEvents();
        }

        buffer += decoder.decode().replace(/\r\n/g, '\n');
        consumeBufferedEvents();

        if (buffer.trim()) {
            const parsedEvent = parseSseEvent(buffer.trim());
            if (parsedEvent?.event === 'done') {
                finalPayload = parsedEvent.data;
            } else if (parsedEvent?.event === 'error') {
                throw new Error(parsedEvent.data?.message || '流式响应失败');
            }
        }

        if (!finalPayload) {
            throw new Error('流式响应已结束，但未收到完成事件');
        }

        return finalPayload;
    }
};

// 页面状态
const MAX_HISTORY_ROUNDS = 4;
const MAX_HISTORY_MESSAGES = MAX_HISTORY_ROUNDS * 2;
let hasConfiguredMarked = false;
let knowledgeBases = [];
let currentUser = null;
let conversationHistory = [];
let selectedKnowledgeBaseIds = []; // 选中的知识库ID列表

// DOM元素引用
function parseSseEvent(rawEvent) {
    const lines = rawEvent.split('\n');
    let event = 'message';
    const dataLines = [];

    lines.forEach(line => {
        if (line.startsWith('event:')) {
            event = line.substring(6).trim();
            return;
        }

        if (line.startsWith('data:')) {
            dataLines.push(line.substring(5).trim());
        }
    });

    if (dataLines.length === 0) {
        return null;
    }

    const rawData = dataLines.join('\n');
    try {
        return {
            event,
            data: JSON.parse(rawData)
        };
    } catch (_) {
        return {
            event,
            data: { content: rawData }
        };
    }
}

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

    let streamingMessage = null;
    try {
        // 添加AI思考中的消息
        streamingMessage = addAiThinkingMessage();
        const history = getRecentConversationHistory();
        let streamedAnswer = '';

        // 调用RAG API
        const answer = await ragAPI.answerStream(cleanMessage, knowledgeBaseIds, history, (chunk) => {
            streamedAnswer += chunk;
            updateAiThinkingMessage(streamingMessage, streamedAnswer);
        });

        // 移除思考中的消息

        // 添加AI回复到聊天界面，并附带相关文档信息
        pushConversationMessage('user', cleanMessage);
        pushConversationMessage('assistant', answer.answer);
        const answerWithReferences = buildAnswerWithReferences(answer);
        updateAiThinkingMessage(streamingMessage, answerWithReferences, true);
    } catch (error) {
        console.error('发送消息失败:', error);
        // 移除思考中的消息
        const thinkingElement = streamingMessage?.messageElement || document.getElementById('ai-thinking-message');
        if (thinkingElement) thinkingElement.remove();

        addMessageToChat(`抱歉，处理您的问题时出现错误: ${error.message}`, 'ai');
    } finally {
        // 恢复发送按钮
        elements.sendBtn.disabled = false;
        elements.sendBtn.innerHTML = '<i class="fas fa-paper-plane"></i> 发送';
    }
}

// 添加消息到聊天界面
function getRecentConversationHistory() {
    return conversationHistory.slice(-MAX_HISTORY_MESSAGES).map(message => ({
        role: message.role,
        content: message.content
    }));
}

function pushConversationMessage(role, content) {
    if (!content) {
        return;
    }

    conversationHistory.push({
        role,
        content
    });

    if (conversationHistory.length > MAX_HISTORY_MESSAGES) {
        conversationHistory = conversationHistory.slice(-MAX_HISTORY_MESSAGES);
    }
}

function buildAnswerWithReferences(answer) {
    let answerWithReferences = answer?.answer || '';

    if (answer.relevantDocuments && answer.relevantDocuments.length > 0) {
        const uniqueDocTitles = [...new Set(
            answer.relevantDocuments.map(doc => doc.noteTitle || '未命名笔记')
        )];
        answerWithReferences += '\n\n---\n### 参考文档\n';
        uniqueDocTitles.forEach((title, index) => {
            answerWithReferences += `${index + 1}. ${title}\n`;
        });
    }

    return answerWithReferences;
}

function addMessageToChat(content, sender) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${sender}-message`;

    const contentElement = document.createElement('div');
    contentElement.className = 'message-content';
    renderMessageContent(contentElement, content, { sender });

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
    contentElement.className = 'message-content markdown-body is-streaming';
    contentElement.innerHTML = `
        <div class="loading-dots">
            <span>●</span>
            <span>●</span>
            <span>●</span>
        </div>
    `;

    const metaElement = document.createElement('div');
    metaElement.className = 'message-meta';

    messageElement.appendChild(contentElement);
    messageElement.appendChild(metaElement);
    elements.chatMessages.appendChild(messageElement);

    // 滚动到底部
    scrollToBottom();

    return {
        messageElement,
        contentElement,
        metaElement
    };
}

function updateAiThinkingMessage(streamingMessage, content, finished = false) {
    if (!streamingMessage || !streamingMessage.contentElement) {
        return;
    }

    renderMessageContent(streamingMessage.contentElement, content || '', {
        sender: 'ai',
        streaming: !finished
    });
    if (finished && streamingMessage.metaElement) {
        streamingMessage.metaElement.textContent = getCurrentTimeString();
        streamingMessage.messageElement.removeAttribute('id');
    }

    scrollToBottom();
}

// 添加相关文档信息
function addRelatedDocuments(documents) {
    // 此函数不再使用，因为我们直接将引用附加到答案中
}

// 格式化消息内容
function renderMessageContent(contentElement, content, options = {}) {
    if (!contentElement) {
        return;
    }

    const {
        sender = 'ai',
        streaming = false
    } = options;
    const safeContent = typeof content === 'string' ? content : String(content ?? '');
    const enableMarkdown = sender === 'ai';

    contentElement.classList.toggle('markdown-body', enableMarkdown);
    contentElement.classList.toggle('is-streaming', enableMarkdown && streaming);

    if (streaming && !safeContent.trim()) {
        contentElement.innerHTML = `
            <div class="loading-dots">
                <span>&#9679;</span>
                <span>&#9679;</span>
                <span>&#9679;</span>
            </div>
        `;
        return;
    }

    contentElement.innerHTML = formatMessageContent(safeContent, { enableMarkdown });
}

function legacyFormatMessageContent(content, options = {}) {
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
function legacyEscapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };

    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function configureMarked() {
    if (hasConfiguredMarked) {
        return;
    }

    marked.setOptions({
        breaks: true,
        gfm: true,
        smartLists: true,
        smartypants: true
    });

    hasConfiguredMarked = true;
}

function enhanceMarkdownHtml(rawHtml) {
    const container = document.createElement('div');
    container.innerHTML = rawHtml;

    container.querySelectorAll('script, iframe, object, embed, form').forEach(element => {
        element.remove();
    });

    container.querySelectorAll('*').forEach(element => {
        [...element.attributes].forEach(attribute => {
            if (/^on/i.test(attribute.name)) {
                element.removeAttribute(attribute.name);
            }
        });
    });

    container.querySelectorAll('a').forEach(link => {
        const href = link.getAttribute('href') || '';
        if (/^\s*javascript:/i.test(href)) {
            link.removeAttribute('href');
            return;
        }

        if (href && !href.startsWith('#')) {
            link.setAttribute('target', '_blank');
            link.setAttribute('rel', 'noopener noreferrer');
        }
    });

    container.querySelectorAll('img').forEach(image => {
        image.setAttribute('loading', 'lazy');
        image.setAttribute('referrerpolicy', 'no-referrer');
        image.setAttribute('alt', image.getAttribute('alt') || 'markdown image');
    });

    container.querySelectorAll('table').forEach(table => {
        if (table.parentElement?.classList.contains('table-wrapper')) {
            return;
        }

        const wrapper = document.createElement('div');
        wrapper.className = 'table-wrapper';
        table.parentNode.insertBefore(wrapper, table);
        wrapper.appendChild(table);
    });

    container.querySelectorAll('pre > code').forEach(codeElement => {
        const preElement = codeElement.parentElement;
        if (!preElement || preElement.parentElement?.classList.contains('code-block')) {
            return;
        }

        const codeBlock = document.createElement('div');
        codeBlock.className = 'code-block';

        const header = document.createElement('div');
        header.className = 'code-block-header';

        const language = document.createElement('span');
        language.className = 'code-language';
        language.textContent = getCodeLanguageLabel(codeElement);

        const copyButton = document.createElement('button');
        copyButton.type = 'button';
        copyButton.className = 'code-copy-btn';
        copyButton.textContent = '复制';

        header.appendChild(language);
        header.appendChild(copyButton);

        preElement.parentNode.insertBefore(codeBlock, preElement);
        codeBlock.appendChild(header);
        codeBlock.appendChild(preElement);
    });

    container.querySelectorAll('p').forEach(paragraph => {
        if (!paragraph.textContent.trim() && paragraph.children.length === 0) {
            paragraph.remove();
        }
    });

    return container.innerHTML;
}

function getCodeLanguageLabel(codeElement) {
    const languageClass = [...codeElement.classList]
        .find(className => className.startsWith('language-'));
    const language = languageClass ? languageClass.replace('language-', '') : '';

    if (!language) {
        return 'TEXT';
    }

    return language.length <= 12 ? language.toUpperCase() : language;
}

async function handleChatMessageActions(event) {
    const copyButton = event.target.closest('.code-copy-btn');
    if (!copyButton) {
        return;
    }

    const codeElement = copyButton.closest('.code-block')?.querySelector('pre > code');
    const codeText = codeElement?.textContent || '';
    if (!codeText) {
        return;
    }

    try {
        await copyTextToClipboard(codeText);
        copyButton.textContent = '已复制';
        copyButton.classList.add('copied');
        window.setTimeout(() => {
            copyButton.textContent = '复制';
            copyButton.classList.remove('copied');
        }, 1600);
    } catch (error) {
        console.warn('复制代码失败:', error);
        DialogUtils.error('复制代码失败，请手动复制', '复制失败');
    }
}

async function copyTextToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        return;
    }

    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.setAttribute('readonly', 'readonly');
    textArea.style.position = 'fixed';
    textArea.style.opacity = '0';
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand('copy');
    document.body.removeChild(textArea);
}

function formatMessageContent(content, options = {}) {
    const { enableMarkdown = true } = options;
    const safeContent = typeof content === 'string' ? content : String(content ?? '');

    if (!enableMarkdown) {
        return escapeHtml(safeContent).replace(/\n/g, '<br>');
    }

    if (typeof marked !== 'undefined') {
        try {
            configureMarked();
            return enhanceMarkdownHtml(marked.parse(safeContent));
        } catch (e) {
            console.warn('Markdown解析失败，使用纯文本显示:', e);
        }
    }

    return escapeHtml(safeContent).replace(/\n/g, '<br>');
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };

    return String(text ?? '').replace(/[&<>"']/g, function(m) { return map[m]; });
}

// 获取当前时间字符串
function buildAnswerWithReferences(answer) {
    let answerWithReferences = answer?.answer || '';

    if (answer.relevantDocuments && answer.relevantDocuments.length > 0) {
        const uniqueDocTitles = [...new Set(
            answer.relevantDocuments.map(doc => doc.noteTitle || '\u672a\u547d\u540d\u7b14\u8bb0')
        )];
        answerWithReferences += '\n\n---\n### \u53c2\u8003\u6587\u6863\n';
        uniqueDocTitles.forEach((title, index) => {
            answerWithReferences += `${index + 1}. ${title}\n`;
        });
    }

    return answerWithReferences;
}

function addMessageToChat(content, sender) {
    const messageElement = document.createElement('div');
    messageElement.className = `message ${sender}-message`;

    const contentElement = document.createElement('div');
    contentElement.className = 'message-content';
    renderMessageContent(contentElement, content, { sender });

    const metaElement = document.createElement('div');
    metaElement.className = 'message-meta';
    metaElement.textContent = getCurrentTimeString();

    messageElement.appendChild(contentElement);
    messageElement.appendChild(metaElement);
    elements.chatMessages.appendChild(messageElement);
    scrollToBottom();
}

function addAiThinkingMessage() {
    const messageElement = document.createElement('div');
    messageElement.className = 'message ai-message';
    messageElement.id = 'ai-thinking-message';

    const contentElement = document.createElement('div');
    contentElement.className = 'message-content markdown-body is-streaming';
    contentElement.innerHTML = `
        <div class="loading-dots">
            <span>&#9679;</span>
            <span>&#9679;</span>
            <span>&#9679;</span>
        </div>
    `;

    const metaElement = document.createElement('div');
    metaElement.className = 'message-meta';

    messageElement.appendChild(contentElement);
    messageElement.appendChild(metaElement);
    elements.chatMessages.appendChild(messageElement);
    scrollToBottom();

    return {
        messageElement,
        contentElement,
        metaElement
    };
}

function updateAiThinkingMessage(streamingMessage, content, finished = false) {
    if (!streamingMessage || !streamingMessage.contentElement) {
        return;
    }

    renderMessageContent(streamingMessage.contentElement, content || '', {
        sender: 'ai',
        streaming: !finished
    });

    if (finished && streamingMessage.metaElement) {
        streamingMessage.metaElement.textContent = getCurrentTimeString();
        streamingMessage.messageElement.removeAttribute('id');
    }

    scrollToBottom();
}

function addRelatedDocuments(documents) {
    return documents;
}

function renderMessageContent(contentElement, content, options = {}) {
    if (!contentElement) {
        return;
    }

    const {
        sender = 'ai',
        streaming = false
    } = options;
    const safeContent = typeof content === 'string' ? content : String(content ?? '');
    const enableMarkdown = sender === 'ai';

    contentElement.classList.toggle('markdown-body', enableMarkdown);
    contentElement.classList.toggle('is-streaming', enableMarkdown && streaming);

    if (streaming && !safeContent.trim()) {
        contentElement.innerHTML = `
            <div class="loading-dots">
                <span>&#9679;</span>
                <span>&#9679;</span>
                <span>&#9679;</span>
            </div>
        `;
        return;
    }

    contentElement.innerHTML = formatMessageContent(safeContent, { enableMarkdown });

    if (enableMarkdown && !streaming) {
        applySyntaxHighlighting(contentElement);
    }
}

function configureMarked() {
    if (hasConfiguredMarked || typeof marked === 'undefined') {
        return;
    }

    marked.setOptions({
        breaks: true,
        gfm: true,
        smartLists: true,
        smartypants: true
    });

    hasConfiguredMarked = true;
}

function enhanceMarkdownHtml(rawHtml) {
    const container = document.createElement('div');
    container.innerHTML = rawHtml;

    container.querySelectorAll('script, iframe, object, embed, form').forEach(element => {
        element.remove();
    });

    container.querySelectorAll('*').forEach(element => {
        [...element.attributes].forEach(attribute => {
            if (/^on/i.test(attribute.name)) {
                element.removeAttribute(attribute.name);
            }
        });
    });

    container.querySelectorAll('a').forEach(link => {
        const href = link.getAttribute('href') || '';
        if (/^\s*javascript:/i.test(href)) {
            link.removeAttribute('href');
            return;
        }

        if (href && !href.startsWith('#')) {
            link.setAttribute('target', '_blank');
            link.setAttribute('rel', 'noopener noreferrer');
        }
    });

    container.querySelectorAll('img').forEach(image => {
        image.setAttribute('loading', 'lazy');
        image.setAttribute('decoding', 'async');
        image.setAttribute('referrerpolicy', 'no-referrer');
        image.setAttribute('alt', image.getAttribute('alt') || 'markdown image');
    });

    container.querySelectorAll('table').forEach(table => {
        if (table.parentElement?.classList.contains('table-wrapper')) {
            return;
        }

        const wrapper = document.createElement('div');
        wrapper.className = 'table-wrapper';
        table.parentNode.insertBefore(wrapper, table);
        wrapper.appendChild(table);
    });

    container.querySelectorAll('p').forEach(paragraph => {
        if (!paragraph.textContent.trim() && paragraph.children.length === 0) {
            paragraph.remove();
        }
    });

    return container.innerHTML;
}

function applySyntaxHighlighting(container) {
    if (!container || typeof hljs === 'undefined') {
        return;
    }

    container.querySelectorAll('pre code').forEach(codeElement => {
        hljs.highlightElement(codeElement);
    });
}

function formatMessageContent(content, options = {}) {
    const { enableMarkdown = true } = options;
    const safeContent = typeof content === 'string' ? content : String(content ?? '');

    if (!enableMarkdown) {
        return escapeHtml(safeContent).replace(/\n/g, '<br>');
    }

    if (typeof marked !== 'undefined') {
        try {
            configureMarked();
            return enhanceMarkdownHtml(marked.parse(safeContent));
        } catch (error) {
            console.warn('Markdown render failed, falling back to plain text:', error);
        }
    }

    return escapeHtml(safeContent).replace(/\n/g, '<br>');
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };

    return String(text ?? '').replace(/[&<>"']/g, function(match) {
        return map[match];
    });
}

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
