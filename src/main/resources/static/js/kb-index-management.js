class DialogUtils {
    static isSwalAvailable() {
        return typeof Swal !== 'undefined';
    }

    static success(message, title = '成功') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'success',
            title,
            text: message,
            confirmButtonText: '确定',
            timer: 3000,
            timerProgressBar: true
        });
    }

    static error(message, title = '错误') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'error',
            title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    static info(message, title = '提示') {
        if (!this.isSwalAvailable()) {
            alert(`${title}: ${message}`);
            return Promise.resolve();
        }
        return Swal.fire({
            icon: 'info',
            title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    static confirm(message, title = '确认') {
        if (!this.isSwalAvailable()) {
            return Promise.resolve({ isConfirmed: confirm(`${title}: ${message}`) });
        }
        return Swal.fire({
            icon: 'question',
            title,
            text: message,
            showCancelButton: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            reverseButtons: true
        });
    }
}

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
            headers.Authorization = `Bearer ${this.token}`;
        }

        return headers;
    }

    async request(url, options = {}) {
        const response = await fetch(`${this.baseURL}${url}`, {
            ...options,
            headers: {
                ...this.getHeaders(!options.isFormData),
                ...options.headers
            }
        });

        if (response.status === 401) {
            this.redirectToLogin();
            throw new Error('认证失败，请重新登录');
        }

        let payload = null;
        try {
            payload = await response.json();
        } catch (_) {
            payload = null;
        }

        if (!response.ok) {
            throw new Error(payload?.message || '请求失败');
        }

        return payload;
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

    async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
}

const apiClient = new ApiClient();

const knowledgeBaseAPI = {
    async getList() {
        const result = await apiClient.get('/knowledge-bases');
        return result.data;
    }
};

const ragAPI = {
    async streamIndexUpdate(knowledgeBaseId, isForce, onProgress) {
        const path = isForce ? '/rag/forceUpdateIndex/stream' : '/rag/updateIndex/stream';
        const response = await fetch(`${apiClient.baseURL}${path}`, {
            method: 'POST',
            headers: apiClient.getHeaders(),
            body: JSON.stringify({ knowledgeBaseId })
        });

        if (response.status === 401) {
            apiClient.redirectToLogin();
            throw new Error('认证失败，请重新登录');
        }

        if (!response.ok) {
            let message = '请求失败';
            try {
                const errorPayload = await response.json();
                message = errorPayload?.message || message;
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

                if (parsedEvent.event === 'progress') {
                    if (typeof onProgress === 'function') {
                        onProgress(parsedEvent.data);
                    }
                    continue;
                }

                if (parsedEvent.event === 'done') {
                    finalPayload = parsedEvent.data;
                    continue;
                }

                if (parsedEvent.event === 'error') {
                    throw new Error(parsedEvent.data?.message || '索引更新失败');
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
                throw new Error(parsedEvent.data?.message || '索引更新失败');
            }
        }

        if (!finalPayload) {
            throw new Error('未收到索引更新完成事件');
        }

        return finalPayload;
    }
};

const state = {
    knowledgeBases: [],
    progressByKbId: new Map()
};

const elements = {
    backToChatBtn: document.getElementById('backToChatBtn'),
    refreshKbBtn: document.getElementById('refreshKbBtn'),
    kbSearch: document.getElementById('kbSearch'),
    knowledgeBases: document.getElementById('knowledgeBases')
};

function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

async function initPage() {
    if (!checkAuth()) {
        return;
    }

    bindEventListeners();
    await loadKnowledgeBases();
}

function bindEventListeners() {
    elements.backToChatBtn.addEventListener('click', () => {
        window.location.href = 'chat.html';
    });

    elements.refreshKbBtn.addEventListener('click', async () => {
        await loadKnowledgeBases();
    });

    elements.kbSearch.addEventListener('input', () => {
        renderKnowledgeBases();
    });
}

async function loadKnowledgeBases(options = {}) {
    const { silent = false } = options;

    try {
        if (!silent) {
            showLoading();
        }
        state.knowledgeBases = await knowledgeBaseAPI.getList();
        renderKnowledgeBases();
    } catch (error) {
        console.error('加载知识库失败:', error);
        await DialogUtils.error(`加载知识库失败: ${error.message}`);
    }
}

function showLoading() {
    elements.knowledgeBases.innerHTML = `
        <div class="kb-empty-state">
            <i class="fas fa-spinner fa-spin fa-2x"></i>
            <p>加载中...</p>
        </div>
    `;
}

function renderKnowledgeBases() {
    const keyword = (elements.kbSearch.value || '').trim().toLowerCase();
    const filteredKbs = state.knowledgeBases.filter(kb => {
        if (!keyword) {
            return true;
        }

        return `${kb.title || ''} ${kb.description || ''}`.toLowerCase().includes(keyword);
    });

    if (!filteredKbs.length) {
        elements.knowledgeBases.innerHTML = `
            <div class="kb-empty-state">
                <i class="fas fa-folder-open fa-3x"></i>
                <h3>${keyword ? '未找到匹配的知识库' : '暂无知识库'}</h3>
                <p>${keyword ? '换个关键词试试。' : '请先创建知识库。'}</p>
            </div>
        `;
        return;
    }

    elements.knowledgeBases.innerHTML = filteredKbs.map(createKnowledgeBaseCard).join('');
    bindKnowledgeBaseActions();
    syncAllProgressCards();
}

function createKnowledgeBaseCard(kb) {
    const progress = state.progressByKbId.get(kb.id);
    const isBusy = Boolean(progress);
    const disableAllActions = isAnyIndexTaskRunning();

    return `
        <div class="kb-card fade-in" data-kb-card-id="${kb.id}">
            <h3>${escapeHtml(kb.title || '未命名知识库')}</h3>
            <p>${escapeHtml(kb.description || '暂无描述')}</p>
            <div class="info">
                <div class="info-item">
                    <span class="info-label">笔记数量:</span>
                    <span class="info-value">${kb.noteCount || 0} 篇</span>
                </div>
                <div class="info-item">
                    <span class="info-label">索引更新时间:</span>
                    <span class="info-value" id="indexTime-${kb.id}">${formatDateTime(kb.indexUpdateTime)}</span>
                </div>
            </div>
            ${createProgressMarkup(kb.id, progress)}
            <div class="actions">
                <button class="btn btn-primary update-index-btn" data-id="${kb.id}" ${disableAllActions ? 'disabled' : ''}>
                    ${isBusy && !progress.forceUpdate ? '<i class="fas fa-spinner fa-spin"></i> 更新中...' : '<i class="fas fa-sync"></i> 增量更新'}
                </button>
                <button class="btn btn-danger force-update-index-btn" data-id="${kb.id}" ${disableAllActions ? 'disabled' : ''}>
                    ${isBusy && progress.forceUpdate ? '<i class="fas fa-spinner fa-spin"></i> 更新中...' : '<i class="fas fa-redo"></i> 强制更新'}
                </button>
            </div>
        </div>
    `;
}

function createProgressMarkup(kbId, progress) {
    if (!progress) {
        return '';
    }

    return `
        <div class="kb-progress is-visible" id="kbProgress-${kbId}">
            <div class="kb-progress-top">
                <span class="kb-progress-message" id="progressMessage-${kbId}">${escapeHtml(progress.message || '准备中...')}</span>
                <span class="kb-progress-percent" id="progressPercent-${kbId}">${formatProgressPercent(progress.progressPercent)}</span>
            </div>
            <div class="kb-progress-bar">
                <div class="kb-progress-fill" id="progressFill-${kbId}" style="width: ${clampProgress(progress.progressPercent)}%;"></div>
            </div>
            <div class="kb-progress-stats" id="progressStats-${kbId}">${escapeHtml(formatProgressStats(progress))}</div>
            <div class="kb-progress-note ${progress.currentNoteTitle ? 'is-visible' : ''}" id="progressNote-${kbId}">
                ${progress.currentNoteTitle ? `当前: ${escapeHtml(progress.currentNoteTitle)}` : ''}
            </div>
        </div>
    `;
}

function bindKnowledgeBaseActions() {
    document.querySelectorAll('.update-index-btn').forEach(button => {
        button.addEventListener('click', async () => {
            const kbId = Number(button.dataset.id);
            await updateIndex(kbId, false);
        });
    });

    document.querySelectorAll('.force-update-index-btn').forEach(button => {
        button.addEventListener('click', async () => {
            const kbId = Number(button.dataset.id);
            await updateIndex(kbId, true);
        });
    });
}

function syncAllProgressCards() {
    state.progressByKbId.forEach((progress, kbId) => {
        syncProgressCard(kbId, progress);
    });
}

function syncProgressCard(kbId, progress = state.progressByKbId.get(kbId)) {
    if (!progress) {
        return;
    }

    const messageElement = document.getElementById(`progressMessage-${kbId}`);
    const percentElement = document.getElementById(`progressPercent-${kbId}`);
    const fillElement = document.getElementById(`progressFill-${kbId}`);
    const statsElement = document.getElementById(`progressStats-${kbId}`);
    const noteElement = document.getElementById(`progressNote-${kbId}`);

    if (!messageElement || !percentElement || !fillElement || !statsElement || !noteElement) {
        return;
    }

    messageElement.textContent = progress.message || '处理中...';
    percentElement.textContent = formatProgressPercent(progress.progressPercent);
    fillElement.style.width = `${clampProgress(progress.progressPercent)}%`;
    statsElement.textContent = formatProgressStats(progress);

    if (progress.currentNoteTitle) {
        noteElement.textContent = `当前: ${progress.currentNoteTitle}`;
        noteElement.classList.add('is-visible');
    } else {
        noteElement.textContent = '';
        noteElement.classList.remove('is-visible');
    }
}

function setProgressState(kbId, progress) {
    state.progressByKbId.set(kbId, normalizeProgress(progress));
}

function clearProgressState(kbId) {
    state.progressByKbId.delete(kbId);
}

function normalizeProgress(progress = {}) {
    return {
        knowledgeBaseId: progress.knowledgeBaseId ?? null,
        forceUpdate: Boolean(progress.forceUpdate),
        stage: progress.stage || 'PREPARING',
        message: progress.message || '准备中...',
        totalNotes: Number(progress.totalNotes ?? 0),
        processedNotes: Number(progress.processedNotes ?? 0),
        insertedCount: Number(progress.insertedCount ?? 0),
        updatedCount: Number(progress.updatedCount ?? 0),
        skippedCount: Number(progress.skippedCount ?? 0),
        deletedCount: Number(progress.deletedCount ?? 0),
        progressPercent: clampProgress(progress.progressPercent ?? 0),
        currentNoteId: progress.currentNoteId ?? null,
        currentNoteTitle: progress.currentNoteTitle || '',
        currentAction: progress.currentAction || ''
    };
}

function isAnyIndexTaskRunning() {
    return state.progressByKbId.size > 0;
}

async function updateIndex(kbId, isForce) {
    if (isAnyIndexTaskRunning()) {
        await DialogUtils.info('当前已有索引任务在执行，请等待完成后再操作。');
        return;
    }

    const kb = state.knowledgeBases.find(item => item.id === kbId);
    if (!kb) {
        await DialogUtils.error('未找到目标知识库');
        return;
    }

    const actionText = isForce ? '强制更新' : '增量更新';
    const confirmResult = await DialogUtils.confirm(
        `确定要对知识库“${kb.title || '未命名知识库'}”执行${actionText}吗？`,
        `${actionText}确认`
    );

    if (!confirmResult.isConfirmed) {
        return;
    }

    setProgressState(kbId, {
        forceUpdate: isForce,
        stage: 'PREPARING',
        message: isForce ? '准备强制更新任务...' : '准备增量更新任务...',
        totalNotes: 0,
        processedNotes: 0,
        insertedCount: 0,
        updatedCount: 0,
        skippedCount: 0,
        deletedCount: 0,
        progressPercent: 0,
        currentNoteTitle: ''
    });
    renderKnowledgeBases();

    try {
        const result = await ragAPI.streamIndexUpdate(kbId, isForce, progress => {
            setProgressState(kbId, progress);
            syncProgressCard(kbId);
        });

        setProgressState(kbId, {
            ...state.progressByKbId.get(kbId),
            stage: 'COMPLETED',
            message: isForce ? '强制更新完成' : '索引更新完成',
            progressPercent: 100,
            totalNotes: state.progressByKbId.get(kbId)?.totalNotes ?? 0,
            processedNotes: state.progressByKbId.get(kbId)?.totalNotes ?? state.progressByKbId.get(kbId)?.processedNotes ?? 0
        });
        syncProgressCard(kbId);

        await loadKnowledgeBases({ silent: true });

        const summary = isForce
            ? `强制更新完成\n重建: ${result.insertedCount} 篇索引`
            : `增量更新完成\n新增: ${result.insertedCount} 篇\n更新: ${result.updatedCount} 篇\n跳过: ${result.skippedCount} 篇\n清理: ${result.deletedCount} 篇`;

        await DialogUtils.success(summary, '索引更新成功');
    } catch (error) {
        console.error('索引更新失败:', error);
        await DialogUtils.error(`索引更新失败: ${error.message}`);
    } finally {
        clearProgressState(kbId);
        renderKnowledgeBases();
    }
}

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

    if (!dataLines.length) {
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

function formatDateTime(dateStr) {
    if (!dateStr) {
        return '从未更新';
    }

    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) {
        return '从未更新';
    }

    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    if (days === 0) {
        return `今天 ${hours}:${minutes}`;
    }
    if (days === 1) {
        return `昨天 ${hours}:${minutes}`;
    }
    if (days < 7) {
        return `${days}天前 ${hours}:${minutes}`;
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function formatProgressStats(progress) {
    const totalNotes = Number(progress.totalNotes ?? 0);
    const processedNotes = Number(progress.processedNotes ?? 0);
    const processedSummary = totalNotes > 0
        ? `进度 ${Math.min(processedNotes, totalNotes)}/${totalNotes}`
        : '等待统计笔记数';

    return `${processedSummary} · 新增 ${progress.insertedCount || 0} · 更新 ${progress.updatedCount || 0} · 跳过 ${progress.skippedCount || 0} · 清理 ${progress.deletedCount || 0}`;
}

function formatProgressPercent(value) {
    return `${clampProgress(value)}%`;
}

function clampProgress(value) {
    const number = Number(value);
    if (Number.isNaN(number)) {
        return 0;
    }
    return Math.max(0, Math.min(100, Math.round(number)));
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

document.addEventListener('DOMContentLoaded', initPage);
