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

const apiClient = new ApiClient();

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

const noteAPI = {
    async getList(knowledgeBaseId) {
        const result = await apiClient.get('/notes', { knowledgeBaseId });
        return result.data;
    },
    async search(knowledgeBaseId, keyword) {
        const result = await apiClient.get('/notes/search', { knowledgeBaseId, keyword });
        return result.data;
    },
    async create(title, content, knowledgeBaseId, groupId = null) {
        const result = await apiClient.post('/notes', { title, content, knowledgeBaseId, groupId });
        return result.data;
    },
    async getById(id) {
        const result = await apiClient.get(`/notes/${id}`);
        return result.data;
    },
    async update(id, title, content, knowledgeBaseId, groupId = null) {
        const result = await apiClient.put(`/notes/${id}`, { title, content, knowledgeBaseId, groupId });
        return result.data;
    },
    async updateGroup(id, groupId) {
        const result = await apiClient.put(`/notes/${id}/group`, { groupId });
        return result.data;
    },
    async delete(id) {
        await apiClient.delete(`/notes/${id}`);
    }
};

const groupAPI = {
    async getList(knowledgeBaseId) {
        const result = await apiClient.get('/note-groups', { knowledgeBaseId });
        return result.data;
    },
    async create(name, knowledgeBaseId, parentId = null) {
        const result = await apiClient.post('/note-groups', { name, knowledgeBaseId, parentId });
        return result.data;
    },
    async update(id, name, knowledgeBaseId, parentId = null) {
        const result = await apiClient.put(`/note-groups/${id}`, { name, knowledgeBaseId, parentId });
        return result.data;
    },
    async move(id, parentId = null) {
        const result = await apiClient.put(`/note-groups/${id}/move`, { parentId });
        return result.data;
    },
    async delete(id) {
        await apiClient.delete(`/note-groups/${id}`);
    }
};
