const urlParams = new URLSearchParams(window.location.search);
const kbId = Number.parseInt(urlParams.get('kbId'), 10);
const kbTitle = urlParams.get('kbTitle');

let notes = [];
let searchResults = [];
let noteGroups = [];
let currentNote = null;
let selectedGroupId = null;
let expandedGroups = new Set();
let isSearchMode = false;
let saveTimeout = null;
let vditor = null;
let lastSidebarWidth = 320;
let selectNoteRequestId = 0;

const elements = {
    sidebar: document.getElementById('sidebar'),
    workspace: document.querySelector('.workspace'),
    notesTree: document.getElementById('notesTree'),
    noteSearch: document.getElementById('noteSearch'),
    addItemBtn: document.getElementById('addItemBtn'),
    backToKb: document.getElementById('backToKb'),
    vditorContainer: document.getElementById('vditorContainer'),
    editorSection: document.getElementById('editorSection'),
    overviewState: document.getElementById('overviewState'),
    collapseBtn: document.getElementById('collapseBtn'),
    currentKbTitle: document.getElementById('currentKbTitle'),
    resizer: document.getElementById('resizer'),
    expandSidebarBtn: document.getElementById('expandSidebarBtn'),
    noteTitleInput: document.getElementById('noteTitleInput'),
    editorMeta: document.getElementById('editorMeta')
};

function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

async function checkParams() {
    if (!kbId || !kbTitle) {
        await DialogUtils.error('缺少必要参数，将返回知识库列表', '参数错误');
        window.location.href = 'dashboard.html';
        return false;
    }
    return true;
}

function getEditorContent() {
    return vditor ? vditor.getValue() : '';
}

function setEditorContent(content) {
    if (vditor) {
        vditor.setValue(content || '');
    }
}

function decodeKbTitle() {
    try {
        return decodeURIComponent(kbTitle || '');
    } catch (error) {
        return kbTitle || '未命名知识库';
    }
}

function escapeHtml(value = '') {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function toTimestamp(value) {
    if (!value) return 0;
    const time = new Date(value).getTime();
    return Number.isNaN(time) ? 0 : time;
}

function formatDateTime(value) {
    if (!value) {
        return '刚刚';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '刚刚';
    }
    return new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

function formatRelativeTime(value) {
    const timestamp = toTimestamp(value);
    if (!timestamp) {
        return '刚刚';
    }

    const diff = Date.now() - timestamp;
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;

    if (diff < minute) {
        return '刚刚';
    }
    if (diff < hour) {
        return `${Math.floor(diff / minute)} 分钟前`;
    }
    if (diff < day) {
        return `${Math.floor(diff / hour)} 小时前`;
    }
    if (diff < 7 * day) {
        return `${Math.floor(diff / day)} 天前`;
    }
    return formatDateTime(value);
}
