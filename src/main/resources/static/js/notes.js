// 自定义提示框工具类
class DialogUtils {
    static success(message, title = '成功') {
        return Swal.fire({
            icon: 'success',
            title: title,
            text: message,
            confirmButtonText: '确定',
            timer: 3000,
            timerProgressBar: true
        });
    }

    static error(message, title = '错误') {
        return Swal.fire({
            icon: 'error',
            title: title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    static warning(message, title = '警告') {
        return Swal.fire({
            icon: 'warning',
            title: title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    static info(message, title = '提示') {
        return Swal.fire({
            icon: 'info',
            title: title,
            text: message,
            confirmButtonText: '确定'
        });
    }

    static confirm(message, title = '确认') {
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

    static async prompt(message, title = '输入', defaultValue = '', placeholder = '') {
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

    static async optionalPrompt(message, title = '输入', defaultValue = '', placeholder = '') {
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

    static loading(message = '处理中...') {
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

    static closeLoading() {
        Swal.close();
    }

    static dangerConfirm(message, title = '危险操作') {
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

    static confirmWithIcon(message, title = '确认', icon = 'question') {
        return Swal.fire({
            icon: icon,
            title: title,
            text: message,
            showCancelButton: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            reverseButtons: true
        });
    }
}

// 保存状态指示器工具类
class SaveIndicator {
    static show(message, type = 'success') {
        const indicator = document.getElementById('saveIndicator');
        const icon = indicator.querySelector('i');
        const text = indicator.querySelector('span');

        text.textContent = message;
        indicator.className = 'save-indicator show ' + type;

        switch (type) {
            case 'success':
                icon.className = 'fas fa-check';
                break;
            case 'error':
                icon.className = 'fas fa-exclamation-triangle';
                break;
            case 'saving':
                icon.className = 'fas fa-spinner fa-spin';
                indicator.className = 'save-indicator show';
                break;
        }

        if (type !== 'saving') {
            setTimeout(() => {
                indicator.classList.remove('show');
            }, 2000);
        }
    }

    static hide() {
        const indicator = document.getElementById('saveIndicator');
        indicator.classList.remove('show');
    }
}

// API客户端类
class ApiClient {
    constructor(baseURL = '/api') {
        this.baseURL = baseURL;
        this.token = localStorage.getItem('authToken') || '';
    }

    getHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }
        return headers;
    }

    async request(url, options = {}) {
        const fullUrl = `${this.baseURL}${url}`;
        const config = {
            ...options,
            headers: { ...this.getHeaders(), ...options.headers }
        };

        try {
            const response = await fetch(fullUrl, config);
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

    async post(url, data) {
        return this.request(url, { method: 'POST', body: JSON.stringify(data) });
    }

    async put(url, data) {
        return this.request(url, { method: 'PUT', body: JSON.stringify(data) });
    }

    async delete(url) {
        return this.request(url, { method: 'DELETE' });
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

// 笔记API
const noteAPI = {
    async getList(knowledgeBaseId) {
        const result = await apiClient.get('/notes', { knowledgeBaseId });
        return result.data;
    },
    async search(knowledgeBaseId, keyword) {
        const result = await apiClient.get('/notes/search', { knowledgeBaseId, keyword });
        return result.data;
    },
    async create(title, content, knowledgeBaseId) {
        const result = await apiClient.post('/notes', { title, content, knowledgeBaseId });
        return result.data;
    },
    async getById(id) {
        const result = await apiClient.get(`/notes/${id}`);
        return result.data;
    },
    async update(id, title, content, knowledgeBaseId) {
        const result = await apiClient.put(`/notes/${id}`, { title, content, knowledgeBaseId });
        return result.data;
    },
    async delete(id) {
        await apiClient.delete(`/notes/${id}`);
    }
};

// 从URL获取参数
const urlParams = new URLSearchParams(window.location.search);
const kbId = parseInt(urlParams.get('kbId'));
const kbTitle = urlParams.get('kbTitle');

// 认证守卫
function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

// 检查必要参数
async function checkParams() {
    if (!kbId || !kbTitle) {
        await DialogUtils.error('缺少必要参数，将返回知识库列表', '参数错误');
        window.location.href = 'dashboard.html';
        return false;
    }
    return true;
}

// 当前状态
let notes = [];
let currentNote = null;
let saveTimeout = null;
let vditor = null;
let originalContent = '';

// DOM元素引用
const elements = {
    sidebar: document.getElementById('sidebar'),
    notesTree: document.getElementById('notesTree'),
    noteSearch: document.getElementById('noteSearch'),
    addNoteBtn: document.getElementById('addNoteBtn'),
    backToKb: document.getElementById('backToKb'),
    vditorContainer: document.getElementById('vditorContainer'),
    editorSection: document.getElementById('editorSection'),
    emptyState: document.getElementById('emptyState'),
    collapseBtn: document.getElementById('collapseBtn'),
    currentKbTitle: document.getElementById('currentKbTitle'),
    resizer: document.getElementById('resizer')
};

// 初始化 Vditor 编辑器（IR 即时渲染模式）
function initVditor(initialContent = '') {
    if (vditor) {
        vditor.destroy();
    }

    vditor = new Vditor('vditorContainer', {
        mode: 'ir',
        height: '100%',
        width: '100%',
        theme: 'classic',
        icon: 'ant',
        toolbar: [
            'emoji', 'headings', 'bold', 'italic', 'strike', 'link', '|',
            'list', 'ordered-list', 'check', 'outdent', 'indent', '|',
            'quote', 'line', 'code', 'inline-code', 'insert-before', 'insert-after', '|',
            'upload', 'table', '|',
            'undo', 'redo', '|',
            'fullscreen', 'edit-mode',
            {
                name: 'more',
                toolbar: ['both', 'code-theme', 'content-theme', 'export', 'outline', 'preview', 'devtools', 'info', 'help'],
            },
        ],
        toolbarConfig: { pin: true },
        counter: { enable: true, type: 'text' },
        cache: { enable: false },
        preview: {
            maxWidth: 9999, // 设置很大的值，让编辑区域宽度接近容器宽度
            markdown: { toc: true, mark: true, footnotes: true, autoSpace: true },
            math: { engine: 'KaTeX' },
            hljs: { lineNumber: true, style: 'github' },
        },
        placeholder: '开始编写您的笔记内容...',
        input: (value) => {
            if (currentNote) {
                triggerAutoSave();
            }
        },
        after: () => {
            if (initialContent) {
                vditor.setValue(initialContent);
            }
            vditor.focus();
            originalContent = initialContent;
            console.log('Vditor 初始化完成');
        },
        upload: {
            accept: 'image/*',
            handler: (files) => {
                for (const file of files) {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        const base64 = e.target.result;
                        vditor.insertValue(`![${file.name}](${base64})`);
                    };
                    reader.readAsDataURL(file);
                }
                return null;
            },
        },
        outline: { enable: false, position: 'left' },
        lang: 'zh_CN',
    });
}

function getEditorContent() {
    return vditor ? vditor.getValue() : '';
}

function setEditorContent(content) {
    if (vditor) {
        vditor.setValue(content);
        originalContent = content;
    }
}

// 初始化页面
async function initPage() {
    if (!checkAuth() || !await checkParams()) return;

    try {
        elements.currentKbTitle.textContent = decodeURIComponent(kbTitle);
        await loadNotes();
    } catch (error) {
        console.error('页面初始化失败:', error);
        if (error.message.includes('认证失败')) return;
        await DialogUtils.error('页面加载失败: ' + error.message);
    }
}

async function loadNotes() {
    try {
        showLoading(true);
        notes = await noteAPI.getList(kbId);
        renderNotesTree();
    } catch (error) {
        console.error('加载笔记失败:', error);
        await DialogUtils.error('加载笔记失败: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function showLoading(show) {
    if (show) {
        elements.notesTree.innerHTML = `
            <div style="text-align: center; padding: 20px; color: #666;">
                <i class="fas fa-spinner fa-spin"></i>
                <p style="margin-top: 10px;">加载中...</p>
            </div>
        `;
    }
}

// 渲染笔记列表
function renderNotesTree(searchTerm = '') {
    let filteredNotes = notes;

    if (searchTerm) {
        filteredNotes = notes.filter(note =>
            note.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (note.content && note.content.toLowerCase().includes(searchTerm.toLowerCase()))
        );
    }

    if (filteredNotes.length === 0) {
        elements.notesTree.innerHTML = `
            <div style="text-align: center; padding: 20px; color: #666;">
                <i class="fas fa-file-alt" style="font-size: 24px; margin-bottom: 10px;"></i>
                <p>暂无笔记</p>
                <p style="font-size: 12px;">点击"新建笔记"创建第一篇笔记</p>
            </div>
        `;
        return;
    }

    elements.notesTree.innerHTML = filteredNotes.map(note => {
        const isActive = currentNote && currentNote.id === note.id;
        return `
            <div class="note-item ${isActive ? 'active' : ''}" data-note-id="${note.id}" onclick="selectNote(${note.id})">
                <i class="note-icon fas fa-file-alt"></i>
                <span class="note-title" ondblclick="event.stopPropagation(); startRenameNote(${note.id})">${note.title}</span>
                <div class="note-actions">
                    <button class="rename-btn" onclick="event.stopPropagation(); startRenameNote(${note.id})" title="重命名">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="delete-btn" onclick="event.stopPropagation(); deleteNote(${note.id}, '${note.title}')" title="删除笔记">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// 开始重命名笔记
function startRenameNote(noteId) {
    const note = notes.find(n => n.id === noteId);
    if (!note) return;

    const noteItem = document.querySelector(`.note-item[data-note-id="${noteId}"]`);
    if (!noteItem) return;

    noteItem.classList.add('editing');

    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'note-title-edit';
    input.value = note.title;

    noteItem.onclick = null;
    noteItem.appendChild(input);
    input.focus();
    input.select();

    const saveRename = async () => {
        const newTitle = input.value.trim();
        if (newTitle && newTitle !== note.title) {
            try {
                SaveIndicator.show('正在保存...', 'saving');

                // 需要先获取笔记完整内容，然后更新标题
                const fullNote = await noteAPI.getById(noteId);
                const updatedNote = await noteAPI.update(note.id, newTitle, fullNote.content, kbId);

                const index = notes.findIndex(n => n.id === note.id);
                if (index !== -1) {
                    notes[index] = updatedNote;
                    if (currentNote && currentNote.id === note.id) {
                        currentNote = updatedNote;
                    }
                }
                SaveIndicator.show('已保存', 'success');
            } catch (error) {
                console.error('重命名失败:', error);
                SaveIndicator.show('保存失败', 'error');
            }
        }
        renderNotesTree();
    };

    input.addEventListener('blur', saveRename);
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            input.blur();
        } else if (e.key === 'Escape') {
            e.preventDefault();
            renderNotesTree();
        }
    });
}

// 选择笔记
async function selectNote(noteId) {
    try {
        if (currentNote && hasUnsavedChanges()) {
            SaveIndicator.show('正在保存...', 'saving');
            await saveCurrentNote();
            SaveIndicator.show('已保存', 'success');
        }

        // 先获取笔记的完整信息（包括内容）
        const fullNote = await noteAPI.getById(noteId);

        currentNote = fullNote;

        if (!vditor) {
            initVditor(fullNote.content || '');
        } else {
            setEditorContent(fullNote.content || '');
        }

        renderNotesTree();
        elements.editorSection.style.display = 'flex';
        elements.emptyState.style.display = 'none';
    } catch (error) {
        console.error('选择笔记失败:', error);
        SaveIndicator.show('保存失败', 'error');
        await DialogUtils.error('加载笔记失败: ' + error.message);
    }
}

function hasUnsavedChanges() {
    if (!currentNote) return false;
    const currentContent = getEditorContent();
    return currentContent !== (currentNote.content || '');
}

async function saveCurrentNote() {
    if (!currentNote) return;

    const content = getEditorContent();

    try {
        const updatedNote = await noteAPI.update(currentNote.id, currentNote.title, content, kbId);
        const index = notes.findIndex(n => n.id === currentNote.id);
        if (index !== -1) {
            notes[index] = updatedNote;
            currentNote = updatedNote;
        }
        originalContent = content;
        console.log('笔记保存成功');
    } catch (error) {
        console.error('保存笔记失败:', error);
        await DialogUtils.error('保存失败: ' + error.message, '保存失败');
        throw error;
    }
}

async function deleteNote(noteId, title) {
    const result = await DialogUtils.dangerConfirm(
        `确定要删除笔记 <strong>"${title}"</strong> 吗？<br><br><span style="color: #dc3545;">⚠️ 此操作不可撤销</span>`,
        '删除笔记'
    );

    if (!result.isConfirmed) return;

    try {
        DialogUtils.loading('正在删除...');
        await noteAPI.delete(noteId);

        notes = notes.filter(n => n.id !== noteId);

        if (currentNote && currentNote.id === noteId) {
            currentNote = null;
            if (vditor) {
                vditor.setValue('');
            }
            elements.editorSection.style.display = 'none';
            elements.emptyState.style.display = 'flex';
        }

        renderNotesTree();
        DialogUtils.closeLoading();
        await DialogUtils.success('笔记删除成功！', '删除成功');

    } catch (error) {
        console.error('删除笔记失败:', error);
        DialogUtils.closeLoading();
        await DialogUtils.error('删除失败: ' + error.message, '删除失败');
    }
}

function triggerAutoSave() {
    if (currentNote && hasUnsavedChanges()) {
        clearTimeout(saveTimeout);
        saveTimeout = setTimeout(async () => {
            try {
                SaveIndicator.show('自动保存中...', 'saving');
                await saveCurrentNote();
                SaveIndicator.show('已自动保存', 'success');
            } catch (error) {
                console.error('自动保存失败:', error);
                SaveIndicator.show('自动保存失败', 'error');
            }
        }, 2000);
    }
}

function setupAutoSave() {
    console.log('自动保存功能已启用');
}

// 折叠侧边栏
elements.collapseBtn.addEventListener('click', () => {
    elements.sidebar.classList.toggle('collapsed');
    const icon = elements.collapseBtn.querySelector('i');
    if (elements.sidebar.classList.contains('collapsed')) {
        icon.className = 'fas fa-chevron-right';
        elements.collapseBtn.title = '展开目录';
        elements.resizer.style.display = 'none';
    } else {
        icon.className = 'fas fa-bars';
        elements.collapseBtn.title = '折叠目录';
        elements.resizer.style.display = 'block';
    }
});

// 拖拽调节侧边栏宽度
(function initResizer() {
    const minWidth = 200;
    const maxWidth = 500;
    let isResizing = false;

    elements.resizer.addEventListener('mousedown', (e) => {
        isResizing = true;
        elements.resizer.classList.add('dragging');
        document.body.classList.add('resizing');
        e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
        if (!isResizing) return;

        let newWidth = e.clientX;
        if (newWidth < minWidth) newWidth = minWidth;
        if (newWidth > maxWidth) newWidth = maxWidth;

        elements.sidebar.style.width = newWidth + 'px';
    });

    document.addEventListener('mouseup', () => {
        if (isResizing) {
            isResizing = false;
            elements.resizer.classList.remove('dragging');
            document.body.classList.remove('resizing');
        }
    });
})();

// 笔记搜索
elements.noteSearch.addEventListener('input', async (e) => {
    const keyword = e.target.value.trim();

    try {
        if (keyword) {
            // 搜索API会返回匹配的笔记（仅ID和标题）
            const searchResults = await noteAPI.search(kbId, keyword);
            notes = searchResults;
        } else {
            await loadNotes();
        }
        renderNotesTree(keyword);
    } catch (error) {
        console.error('搜索笔记失败:', error);
        renderNotesTree(keyword);
    }
});

// 新建笔记
elements.addNoteBtn.addEventListener('click', async () => {
    const title = await DialogUtils.prompt('请输入笔记标题', '新建笔记', '', '例如：学习笔记');
    if (!title) return;

    try {
        elements.addNoteBtn.disabled = true;
        elements.addNoteBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 创建中...';

        const newNote = await noteAPI.create(
            title.trim(),
            `# ${title.trim()}\n\n开始编写您的笔记内容...`,
            kbId
        );

        notes.unshift(newNote);
        renderNotesTree();
        await selectNote(newNote.id);

        await DialogUtils.success('笔记创建成功！', '创建成功');

    } catch (error) {
        console.error('创建笔记失败:', error);
        await DialogUtils.error('创建失败: ' + error.message, '创建失败');
    } finally {
        elements.addNoteBtn.disabled = false;
        elements.addNoteBtn.innerHTML = '<i class="fas fa-plus"></i> 新建笔记';
    }
});

// 返回知识库列表
elements.backToKb.addEventListener('click', async () => {
    try {
        if (currentNote && hasUnsavedChanges()) {
            SaveIndicator.show('正在保存...', 'saving');
            await saveCurrentNote();
            SaveIndicator.show('已保存', 'success');
        }
        window.location.href = 'dashboard.html';
    } catch (error) {
        const result = await DialogUtils.confirmWithIcon(
            '当前笔记有未保存的更改，是否放弃更改并返回？',
            '未保存的更改',
            'warning'
        );
        if (result.isConfirmed) {
            window.location.href = 'dashboard.html';
        }
    }
});

// 页面卸载前保存
window.addEventListener('beforeunload', async (e) => {
    if (currentNote && hasUnsavedChanges()) {
        try {
            await saveCurrentNote();
        } catch (error) {
            e.preventDefault();
            e.returnValue = '您有未保存的更改，确定要离开吗？';
            return e.returnValue;
        }
    }
});

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    initPage();
    setupAutoSave();
});