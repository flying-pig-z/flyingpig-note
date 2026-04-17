function buildBreadcrumbHtml(groups, currentLabel) {
    const items = [
        `<button class="breadcrumb-link" onclick="selectLibraryHome()">知识库</button>`
    ];

    groups.forEach((group) => {
        items.push(`<span class="breadcrumb-separator">/</span>`);
        items.push(`<button class="breadcrumb-link" onclick="selectGroup(${group.id})">${escapeHtml(group.name)}</button>`);
    });

    if (currentLabel) {
        items.push(`<span class="breadcrumb-separator">/</span>`);
        items.push(`<span class="breadcrumb-current">${escapeHtml(currentLabel)}</span>`);
    }

    return items.join('');
}

function toggleWorkspacePanels(mode) {
    const isEditor = mode === 'editor';
    elements.editorSection.style.display = isEditor ? 'flex' : 'none';
    elements.overviewState.style.display = isEditor ? 'none' : 'block';
}

function renderStatCards(cards) {
    return cards.map((card) => `
        <div class="overview-card">
            <div class="overview-card-label">${escapeHtml(card.label)}</div>
            <div class="overview-card-value">${escapeHtml(String(card.value))}</div>
            <div class="overview-card-foot">${escapeHtml(card.foot)}</div>
        </div>
    `).join('');
}

function renderOverviewGroupList(groups) {
    if (groups.length === 0) {
        return '<div class="empty-list">这里还没有下级分组。</div>';
    }

    return `
        <div class="overview-list">
            ${groups.map((group) => `
                <button class="overview-link-card" onclick="selectGroup(${group.id})">
                    <span class="overview-link-icon"><i class="fas fa-folder"></i></span>
                    <span class="overview-link-copy">
                        <strong>${escapeHtml(group.name)}</strong>
                        <small>点击进入分组</small>
                    </span>
                </button>
            `).join('')}
        </div>
    `;
}

function renderOverviewNoteList(items) {
    if (items.length === 0) {
        return '<div class="empty-list">这里还没有文档。</div>';
    }

    return `
        <div class="overview-list">
            ${items.map((note) => `
                <button class="overview-link-card" onclick="selectNote(${note.id})">
                    <span class="overview-link-icon"><i class="fas fa-file-lines"></i></span>
                    <span class="overview-link-copy">
                        <strong>${escapeHtml(note.title || '未命名文档')}</strong>
                        <small>${escapeHtml(getGroupPathLabel(note.groupId))} · ${escapeHtml(formatRelativeTime(note.updateTime))}</small>
                    </span>
                </button>
            `).join('')}
        </div>
    `;
}

function showHomeOverview() {
    const { rootGroups, rootNotes } = buildGroupTree();
    const recentNotes = [...notes].sort(sortByUpdatedTimeDesc).slice(0, 6);

    toggleWorkspacePanels('overview');
    elements.overviewState.innerHTML = `
        <div class="overview-hero">
            <div class="overview-kicker">
                <i class="fas fa-book"></i>
                <span>知识库目录</span>
            </div>
            <h1>${escapeHtml(decodeKbTitle())}</h1>
            <div class="overview-actions">
                <button class="primary-btn" onclick="createNoteWithGroup(null)">新建文档</button>
                <button class="secondary-btn" onclick="createGroup(null)">新建分组</button>
            </div>
        </div>
        <div class="overview-grid">
            ${renderStatCards([
                { label: '全部分组', value: noteGroups.length, foot: '树状目录结构' },
                { label: '全部文档', value: notes.length, foot: '知识库总文档数' },
                { label: '根目录文档', value: rootNotes.length, foot: '未放入分组的文档' }
            ])}
        </div>
        <div class="overview-section">
            <div class="overview-section-head">
                <h2>一级分组</h2>
                <span>${rootGroups.length} 个</span>
            </div>
            ${renderOverviewGroupList(rootGroups)}
        </div>
        <div class="overview-section">
            <div class="overview-section-head">
                <h2>最近更新</h2>
                <span>${recentNotes.length} 篇</span>
            </div>
            ${renderOverviewNoteList(recentNotes)}
        </div>
    `;
}

function showGroupOverview(groupId) {
    const summary = getGroupSummary(groupId);
    if (!summary) {
        selectedGroupId = null;
        showHomeOverview();
        return;
    }

    toggleWorkspacePanels('overview');
    elements.overviewState.innerHTML = `
        <div class="overview-hero">
            <div class="overview-kicker">
                <i class="fas fa-folder-open"></i>
                <span>分组目录</span>
            </div>
            <div class="overview-breadcrumb">${buildBreadcrumbHtml(summary.path.slice(0, -1), summary.group.name)}</div>
            <h1>${escapeHtml(summary.group.name)}</h1>
            <div class="overview-actions">
                <button class="primary-btn" onclick="createNoteWithGroup(${summary.group.id})">新建文档</button>
                <button class="secondary-btn" onclick="createGroup(${summary.group.id})">新建子分组</button>
                <button class="ghost-btn" onclick="startRenameGroup(${summary.group.id})">重命名</button>
                <button class="ghost-btn danger" onclick="deleteGroup(${summary.group.id})">删除</button>
            </div>
        </div>
        <div class="overview-grid">
            ${renderStatCards([
                { label: '直接子分组', value: summary.directGroupCount, foot: '当前层级' },
                { label: '直接文档', value: summary.directNoteCount, foot: '当前分组下的文档' },
                { label: '全部下级文档', value: summary.descendantNoteCount, foot: '包含所有子孙分组' }
            ])}
        </div>
        <div class="overview-section">
            <div class="overview-section-head">
                <h2>下级分组</h2>
                <span>${summary.directGroupCount} 个</span>
            </div>
            ${renderOverviewGroupList(summary.group.childrenGroups)}
        </div>
        <div class="overview-section">
            <div class="overview-section-head">
                <h2>当前分组文档</h2>
                <span>${summary.directNoteCount} 篇</span>
            </div>
            ${renderOverviewNoteList(summary.group.childrenNotes)}
        </div>
    `;
}

function refreshWorkspaceContext() {
    if (currentNote) {
        showNoteEditor(currentNote);
        return;
    }

    if (selectedGroupId != null) {
        showGroupOverview(selectedGroupId);
        return;
    }

    showHomeOverview();
}

async function initPage() {
    if (!checkAuth() || !await checkParams()) {
        return;
    }

    try {
        elements.currentKbTitle.textContent = decodeKbTitle();
        await loadNotes();
    } catch (error) {
        console.error('页面初始化失败:', error);
        if (error.message.includes('认证失败')) {
            return;
        }
        await DialogUtils.error(`页面加载失败: ${error.message}`);
    }
}

async function loadNotes() {
    try {
        showLoading(true);
        const [noteList, groupList] = await Promise.all([
            noteAPI.getList(kbId),
            groupAPI.getList(kbId)
        ]);

        notes = Array.isArray(noteList) ? noteList : [];
        noteGroups = Array.isArray(groupList) ? groupList : [];
        searchResults = [];
        isSearchMode = false;

        if (expandedGroups.size === 0) {
            expandedGroups = new Set(
                noteGroups
                    .filter((group) => group.parentId == null)
                    .map((group) => group.id)
            );
        }

        renderNotesTree();
        refreshWorkspaceContext();
    } catch (error) {
        console.error('加载文档失败:', error);
        elements.notesTree.innerHTML = `
            <div class="tree-empty">
                <i class="fas fa-triangle-exclamation"></i>
                <h3>目录加载失败</h3>
                <p>请稍后重试，或者返回知识库列表重新进入。</p>
            </div>
        `;
        await DialogUtils.error(`加载文档失败: ${error.message}`);
    } finally {
        showLoading(false);
    }
}

function showLoading(show) {
    if (!show) {
        return;
    }

    elements.notesTree.innerHTML = `
        <div class="tree-empty">
            <i class="fas fa-spinner fa-spin"></i>
            <h3>正在加载知识库</h3>
            <p>目录和文档马上就绪。</p>
        </div>
    `;
}

function syncSidebarLayoutState() {
    const isCollapsed = elements.sidebar.classList.contains('collapsed');
    if (elements.workspace) {
        elements.workspace.classList.toggle('sidebar-collapsed', isCollapsed);
    }
}

function collapseSidebar() {
    lastSidebarWidth = elements.sidebar.offsetWidth || lastSidebarWidth;
    elements.sidebar.classList.add('collapsed');
    elements.sidebar.style.width = '';
    elements.resizer.style.display = 'none';
    elements.expandSidebarBtn.style.display = 'inline-flex';
    syncSidebarLayoutState();
}

function expandSidebar() {
    elements.sidebar.classList.remove('collapsed');
    elements.sidebar.style.width = `${lastSidebarWidth || 320}px`;
    elements.resizer.style.display = 'block';
    elements.expandSidebarBtn.style.display = 'none';
    syncSidebarLayoutState();
}

elements.collapseBtn.addEventListener('click', () => {
    if (elements.sidebar.classList.contains('collapsed')) {
        expandSidebar();
    } else {
        collapseSidebar();
    }
});

elements.expandSidebarBtn.addEventListener('click', () => {
    expandSidebar();
});

(function initResizer() {
    const minWidth = 260;
    const maxWidth = 480;
    let isResizing = false;

    elements.resizer.addEventListener('mousedown', (event) => {
        isResizing = true;
        elements.resizer.classList.add('dragging');
        document.body.classList.add('resizing');
        event.preventDefault();
    });

    document.addEventListener('mousemove', (event) => {
        if (!isResizing) {
            return;
        }

        const containerRect = document.querySelector('.notes-shell').getBoundingClientRect();
        let nextWidth = event.clientX - containerRect.left;
        nextWidth = Math.max(minWidth, Math.min(maxWidth, nextWidth));

        elements.sidebar.style.width = `${nextWidth}px`;
        lastSidebarWidth = nextWidth;
    });

    document.addEventListener('mouseup', () => {
        if (!isResizing) {
            return;
        }

        isResizing = false;
        elements.resizer.classList.remove('dragging');
        document.body.classList.remove('resizing');
    });
})();

elements.noteSearch.addEventListener('input', async (event) => {
    const keyword = event.target.value.trim();

    try {
        if (!keyword) {
            isSearchMode = false;
            searchResults = [];
            renderNotesTree();
            return;
        }

        isSearchMode = true;
        searchResults = await noteAPI.search(kbId, keyword);
        renderNotesTree();
    } catch (error) {
        console.error('搜索文档失败:', error);
        await DialogUtils.error(`搜索失败: ${error.message}`);
    }
});

elements.addItemBtn.addEventListener('click', (event) => {
    event.stopPropagation();
    toggleActionMenu('add', 'item', '');
});

elements.backToKb.addEventListener('click', async () => {
    try {
        await persistCurrentNoteIfNeeded();
        window.location.href = 'dashboard.html';
    } catch (error) {
        const result = await DialogUtils.confirmWithIcon(
            '当前文档还有未保存的内容，是否放弃更改并返回？',
            '未保存的更改',
            'warning'
        );
        if (result.isConfirmed) {
            window.location.href = 'dashboard.html';
        }
    }
});

window.addEventListener('beforeunload', async (event) => {
    if (!currentNote || !hasUnsavedChanges()) {
        return;
    }

    try {
        await saveCurrentNote();
    } catch (error) {
        event.preventDefault();
        event.returnValue = '你有未保存的更改，确定离开吗？';
        return event.returnValue;
    }
});

document.addEventListener('DOMContentLoaded', () => {
    initPage();
    setupAutoSave();
    setupTitleInputAutoSave();
    syncSidebarLayoutState();
});

document.addEventListener('click', () => {
    closeAllMenus();
});
