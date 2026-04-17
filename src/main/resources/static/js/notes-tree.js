function sortByCreatedTime(left, right) {
    const timestampDiff = toTimestamp(left.createTime) - toTimestamp(right.createTime);
    if (timestampDiff !== 0) {
        return timestampDiff;
    }
    return (left.id || 0) - (right.id || 0);
}

function sortByUpdatedTimeDesc(left, right) {
    const timestampDiff = toTimestamp(right.updateTime) - toTimestamp(left.updateTime);
    if (timestampDiff !== 0) {
        return timestampDiff;
    }
    return (right.id || 0) - (left.id || 0);
}

function findNoteById(noteId) {
    return notes.find((item) => item.id === noteId) || searchResults.find((item) => item.id === noteId) || null;
}

function findGroupById(groupId) {
    return noteGroups.find((item) => item.id === groupId) || null;
}

function updateNoteCollections(noteId, updater) {
    notes = notes.map((item) => item.id === noteId ? updater(item) : item);
    searchResults = searchResults.map((item) => item.id === noteId ? updater(item) : item);
}

function removeNoteFromCollections(noteId) {
    notes = notes.filter((item) => item.id !== noteId);
    searchResults = searchResults.filter((item) => item.id !== noteId);
}

function updateGroupCollections(groupId, updater) {
    noteGroups = noteGroups.map((item) => item.id === groupId ? updater(item) : item);
}

function removeGroupFromCollections(groupId) {
    noteGroups = noteGroups.filter((item) => item.id !== groupId);
}

function exitSearchMode() {
    isSearchMode = false;
    searchResults = [];
    if (elements.noteSearch) {
        elements.noteSearch.value = '';
    }
}

function buildGroupTree() {
    const groupMap = new Map();
    const sortedGroups = [...noteGroups].sort(sortByCreatedTime);
    const sortedNotes = [...notes].sort(sortByCreatedTime);

    sortedGroups.forEach((group) => {
        groupMap.set(group.id, {
            ...group,
            childrenGroups: [],
            childrenNotes: []
        });
    });

    const rootGroups = [];
    sortedGroups.forEach((group) => {
        const node = groupMap.get(group.id);
        if (group.parentId && groupMap.has(group.parentId)) {
            groupMap.get(group.parentId).childrenGroups.push(node);
        } else {
            rootGroups.push(node);
        }
    });

    const rootNotes = [];
    sortedNotes.forEach((note) => {
        if (note.groupId && groupMap.has(note.groupId)) {
            groupMap.get(note.groupId).childrenNotes.push(note);
        } else {
            rootNotes.push(note);
        }
    });

    return { rootGroups, rootNotes, groupMap };
}

function getGroupPath(groupId) {
    if (!groupId) {
        return [];
    }

    const groupMap = new Map(noteGroups.map((group) => [group.id, group]));
    const path = [];
    let currentId = groupId;

    while (currentId && groupMap.has(currentId)) {
        const group = groupMap.get(currentId);
        path.unshift(group);
        currentId = group.parentId;
    }

    return path;
}

function getGroupPathLabel(groupId) {
    const path = getGroupPath(groupId).map((group) => group.name);
    return path.length > 0 ? path.join(' / ') : '根目录';
}

function countDescendantGroups(groupNode) {
    return groupNode.childrenGroups.reduce(
        (total, child) => total + 1 + countDescendantGroups(child),
        0
    );
}

function countDescendantNotes(groupNode) {
    return groupNode.childrenGroups.reduce(
        (total, child) => total + countDescendantNotes(child),
        groupNode.childrenNotes.length
    );
}

function getGroupSummary(groupId) {
    const { groupMap } = buildGroupTree();
    const groupNode = groupMap.get(groupId);

    if (!groupNode) {
        return null;
    }

    return {
        group: groupNode,
        path: getGroupPath(groupId),
        directGroupCount: groupNode.childrenGroups.length,
        directNoteCount: groupNode.childrenNotes.length,
        descendantGroupCount: countDescendantGroups(groupNode),
        descendantNoteCount: countDescendantNotes(groupNode)
    };
}

function renderNotesTree() {
    if (isSearchMode) {
        renderSearchTree();
        return;
    }

    const { rootGroups, rootNotes } = buildGroupTree();
    const activePathIds = new Set(currentNote ? getGroupPath(currentNote.groupId).map((item) => item.id) : []);

    if (rootGroups.length === 0 && rootNotes.length === 0) {
        elements.notesTree.innerHTML = `
            <div class="tree-empty">
                <i class="fas fa-book-open"></i>
                <h3>知识库还是空的</h3>
                <p>先创建一级分组，或者直接写第一篇文档。</p>
                <div class="tree-empty-actions">
                    <button class="secondary-btn" onclick="createGroup(null)">新建分组</button>
                    <button class="primary-btn" onclick="createNoteWithGroup(null)">新建文档</button>
                </div>
            </div>
        `;
        return;
    }

    elements.notesTree.innerHTML = `
        ${renderLibraryRoot()}
        <div class="tree-collection">
            ${rootGroups.map((group) => renderGroupNode(group, 0, activePathIds)).join('')}
            ${rootNotes.map((note) => renderNoteNode(note, 0, { compact: false })).join('')}
        </div>
    `;
}

function renderSearchTree() {
    const keyword = elements.noteSearch.value.trim();
    const resultNotes = [...searchResults].sort(sortByUpdatedTimeDesc);

    if (resultNotes.length === 0) {
        elements.notesTree.innerHTML = `
            <div class="tree-empty">
                <i class="fas fa-search"></i>
                <h3>没有找到相关文档</h3>
                <p>${escapeHtml(keyword)} 没有匹配结果，试试别的关键词。</p>
            </div>
        `;
        return;
    }

    elements.notesTree.innerHTML = `
        <div class="tree-section-title">搜索结果</div>
        <div class="tree-search-summary">共找到 ${resultNotes.length} 篇文档</div>
        <div class="tree-collection">
            ${resultNotes.map((note) => renderNoteNode(note, 0, { compact: true, showPath: true })).join('')}
        </div>
    `;
}

function renderLibraryRoot() {
    const isActive = !currentNote && selectedGroupId == null;
    return `
        <div class="tree-home-row ${isActive ? 'active' : ''}"
            ondragover="handleDragOver(event)"
            ondragleave="handleDragLeave(event)"
            ondrop="handleDropOnRoot(event)"
            onclick="selectLibraryHome()">
            <div class="tree-home-main">
                <div class="tree-home-icon">
                    <i class="fas fa-book"></i>
                </div>
                <div class="tree-home-title">知识库根目录</div>
            </div>
        </div>
    `;
}

function renderGroupNode(group, level, activePathIds) {
    const childCount = group.childrenGroups.length + group.childrenNotes.length;
    const isOpen = expandedGroups.has(group.id);
    const isSelected = !currentNote && selectedGroupId === group.id;
    const isAncestor = currentNote ? activePathIds.has(group.id) : false;
    const hasChildren = childCount > 0;

    return `
        <div class="tree-node group-node">
            <div class="tree-row group-row ${isSelected ? 'active' : ''} ${isAncestor ? 'ancestor' : ''}"
                style="--level:${level};"
                draggable="true"
                ondragstart="startDrag(event, 'group', ${group.id})"
                ondragover="handleDragOver(event)"
                ondragleave="handleDragLeave(event)"
                ondrop="handleDropOnGroup(event, ${group.id})"
                onclick="selectGroup(${group.id})">
                <button class="tree-toggle ${hasChildren ? '' : 'placeholder'}"
                    onclick="event.stopPropagation(); toggleGroup(${group.id})"
                    ${hasChildren ? '' : 'tabindex="-1"'}>
                    <i class="fas ${isOpen ? 'fa-chevron-down' : 'fa-chevron-right'}"></i>
                </button>
                <i class="fas ${isOpen ? 'fa-folder-open' : 'fa-folder'} tree-icon folder"></i>
                <span class="tree-label">${escapeHtml(group.name)}</span>
                <div class="item-actions">
                    <div class="menu-trigger">
                        <button class="row-action" title="更多操作" onclick="event.stopPropagation(); toggleActionMenu('group', ${group.id}, 'more')">
                            <i class="fas fa-ellipsis-h"></i>
                        </button>
                        <div id="group-${group.id}-more-menu" class="action-menu menu-right" onclick="event.stopPropagation();">
                            <button onclick="event.stopPropagation(); createGroup(${group.id}); closeAllMenus();">新建子分组</button>
                            <button onclick="event.stopPropagation(); createNoteWithGroup(${group.id}); closeAllMenus();">新建文档</button>
                            <button onclick="event.stopPropagation(); startRenameGroup(${group.id}); closeAllMenus();">重命名</button>
                            <button class="danger" onclick="event.stopPropagation(); deleteGroup(${group.id}); closeAllMenus();">删除分组</button>
                        </div>
                    </div>
                </div>
            </div>
            ${isOpen ? `
                <div class="tree-children">
                    ${group.childrenGroups.map((child) => renderGroupNode(child, level + 1, activePathIds)).join('')}
                    ${group.childrenNotes.map((note) => renderNoteNode(note, level + 1, { compact: false })).join('')}
                </div>
            ` : ''}
        </div>
    `;
}

function renderNoteNode(note, level, options = {}) {
    const isActive = currentNote && currentNote.id === note.id;
    const parentGroupId = note.groupId == null ? 'null' : note.groupId;
    const subtitle = options.showPath
        ? `<div class="tree-note-meta">${escapeHtml(getGroupPathLabel(note.groupId))}</div>`
        : '';

    return `
        <div class="tree-node note-node">
            <div class="tree-row note-row ${isActive ? 'active' : ''} ${options.compact ? 'compact' : ''}"
                style="--level:${level};"
                data-note-id="${note.id}"
                draggable="true"
                ondragstart="startDrag(event, 'note', ${note.id})"
                onclick="selectNote(${note.id})">
                <i class="fas fa-file-lines tree-icon note"></i>
                <div class="tree-note-copy">
                    <span class="tree-label note-title">${escapeHtml(note.title || '未命名文档')}</span>
                    ${subtitle}
                </div>
                <div class="item-actions">
                    <div class="menu-trigger">
                        <button class="row-action" title="更多操作" onclick="event.stopPropagation(); toggleActionMenu('note', ${note.id}, 'more')">
                            <i class="fas fa-ellipsis-h"></i>
                        </button>
                        <div id="note-${note.id}-more-menu" class="action-menu menu-right" onclick="event.stopPropagation();">
                            <button onclick="event.stopPropagation(); createNoteWithGroup(${parentGroupId}); closeAllMenus();">新建同级文档</button>
                            <button onclick="event.stopPropagation(); createGroup(${parentGroupId}); closeAllMenus();">新建同级分组</button>
                            <button onclick="event.stopPropagation(); startRenameNote(${note.id}); closeAllMenus();">重命名</button>
                            <button class="danger" onclick="event.stopPropagation(); deleteNote(${note.id}); closeAllMenus();">删除文档</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

async function persistCurrentNoteIfNeeded() {
    if (currentNote && hasUnsavedChanges()) {
        SaveIndicator.show('正在保存...', 'saving');
        await saveCurrentNote();
        SaveIndicator.show('已保存', 'success');
    }
}

async function selectLibraryHome() {
    try {
        await persistCurrentNoteIfNeeded();
        currentNote = null;
        selectedGroupId = null;
        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
    } catch (error) {
        console.error('切换知识库根目录失败:', error);
        SaveIndicator.show('保存失败', 'error');
    }
}

async function selectGroup(groupId) {
    try {
        await persistCurrentNoteIfNeeded();
        currentNote = null;
        selectedGroupId = groupId;
        ensureGroupVisible(groupId);
        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
    } catch (error) {
        console.error('切换分组失败:', error);
        SaveIndicator.show('保存失败', 'error');
    }
}

function toggleGroup(groupId) {
    if (expandedGroups.has(groupId)) {
        expandedGroups.delete(groupId);
    } else {
        expandedGroups.add(groupId);
    }
    renderNotesTree();
}

function ensureGroupVisible(groupId) {
    if (!groupId) {
        return false;
    }

    let didExpand = false;
    let currentId = groupId;
    const groupMap = new Map(noteGroups.map((group) => [group.id, group]));

    while (currentId && groupMap.has(currentId)) {
        if (!expandedGroups.has(currentId)) {
            expandedGroups.add(currentId);
            didExpand = true;
        }
        currentId = groupMap.get(currentId).parentId;
    }

    return didExpand;
}

async function createGroup(parentId = null) {
    const dialogTitle = parentId ? '新建子分组' : '新建分组';
    const name = await DialogUtils.prompt('请输入分组名称', dialogTitle, '', '例如：接口设计');
    if (!name) {
        return;
    }

    try {
        exitSearchMode();
        const group = await groupAPI.create(name.trim(), kbId, parentId);
        noteGroups.push(group);
        expandedGroups.add(group.id);
        if (parentId) {
            expandedGroups.add(parentId);
        }
        currentNote = null;
        selectedGroupId = group.id;
        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
        await DialogUtils.success('分组创建成功', '创建成功');
    } catch (error) {
        console.error('创建分组失败:', error);
        await DialogUtils.error(`创建分组失败: ${error.message}`);
    }
}

async function createNoteWithGroup(groupId = null) {
    const title = await DialogUtils.prompt('请输入文档标题', '新建文档', '', '例如：接口约定');
    if (!title) {
        return;
    }

    try {
        exitSearchMode();
        const trimmedTitle = title.trim();
        const newNote = await noteAPI.create(
            trimmedTitle,
            `# ${trimmedTitle}\n\n开始编写这篇文档吧。`,
            kbId,
            groupId
        );

        notes.push(newNote);
        if (groupId) {
            ensureGroupVisible(groupId);
        }
        renderNotesTree();
        await selectNote(newNote.id);
        await DialogUtils.success('文档创建成功', '创建成功');
    } catch (error) {
        console.error('创建文档失败:', error);
        await DialogUtils.error(`创建文档失败: ${error.message}`);
    }
}

async function startRenameGroup(groupId) {
    const group = findGroupById(groupId);
    if (!group) {
        return;
    }

    const newName = await DialogUtils.prompt('请输入新的分组名称', '重命名分组', group.name, '例如：项目管理');
    if (!newName || newName.trim() === group.name) {
        return;
    }

    try {
        const updated = await groupAPI.update(groupId, newName.trim(), kbId, group.parentId);
        updateGroupCollections(groupId, () => updated);
        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
        await DialogUtils.success('分组已更新', '更新成功');
    } catch (error) {
        console.error('重命名分组失败:', error);
        await DialogUtils.error(`重命名失败: ${error.message}`);
    }
}

async function deleteGroup(groupId) {
    const group = findGroupById(groupId);
    if (!group) {
        return;
    }

    const result = await DialogUtils.dangerConfirm(
        `确定删除分组 <strong>${escapeHtml(group.name)}</strong> 吗？<br><br><span style="color:#dc3545;">子分组和文档会自动挂到上级目录。</span>`,
        '删除分组'
    );

    if (!result.isConfirmed) {
        return;
    }

    try {
        const parentId = group.parentId ?? null;
        await groupAPI.delete(groupId);

        removeGroupFromCollections(groupId);
        noteGroups = noteGroups.map((item) => item.parentId === groupId ? { ...item, parentId } : item);
        updateNoteCollectionsForGroupRemoval(groupId, parentId);

        if (selectedGroupId === groupId) {
            selectedGroupId = parentId;
        }
        if (currentNote && currentNote.groupId === groupId) {
            currentNote = { ...currentNote, groupId: parentId };
            selectedGroupId = parentId;
        }

        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
        await DialogUtils.success('分组已删除', '删除成功');
    } catch (error) {
        console.error('删除分组失败:', error);
        await DialogUtils.error(`删除失败: ${error.message}`);
    }
}

function updateNoteCollectionsForGroupRemoval(groupId, parentId) {
    notes = notes.map((item) => item.groupId === groupId ? { ...item, groupId: parentId } : item);
    searchResults = searchResults.map((item) => item.groupId === groupId ? { ...item, groupId: parentId } : item);
}

async function startRenameNote(noteId) {
    const note = findNoteById(noteId);
    if (!note) {
        return;
    }

    const newTitle = await DialogUtils.prompt('请输入新的文档标题', '重命名文档', note.title, '例如：部署说明');
    if (!newTitle || newTitle.trim() === note.title) {
        return;
    }

    try {
        const baseNote = currentNote && currentNote.id === noteId
            ? { ...currentNote, title: getDraftTitle(), content: getEditorContent() }
            : await noteAPI.getById(noteId);
        const updated = await noteAPI.update(
            noteId,
            newTitle.trim(),
            baseNote.content,
            kbId,
            baseNote.groupId
        );

        updateNoteCollections(noteId, () => updated);
        if (currentNote && currentNote.id === noteId) {
            currentNote = updated;
        }

        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
        await DialogUtils.success('文档已更新', '更新成功');
    } catch (error) {
        console.error('重命名文档失败:', error);
        await DialogUtils.error(`重命名失败: ${error.message}`);
    }
}

async function deleteNote(noteId) {
    const note = findNoteById(noteId);
    if (!note) {
        return;
    }

    const result = await DialogUtils.dangerConfirm(
        `确定删除文档 <strong>${escapeHtml(note.title || '未命名文档')}</strong> 吗？<br><br><span style="color:#dc3545;">此操作不可撤销。</span>`,
        '删除文档'
    );

    if (!result.isConfirmed) {
        return;
    }

    try {
        await noteAPI.delete(noteId);
        removeNoteFromCollections(noteId);

        if (currentNote && currentNote.id === noteId) {
            currentNote = null;
            selectedGroupId = note.groupId ?? null;
        }

        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
        await DialogUtils.success('文档已删除', '删除成功');
    } catch (error) {
        console.error('删除文档失败:', error);
        await DialogUtils.error(`删除失败: ${error.message}`);
    }
}

function startDrag(event, type, id) {
    if (type === 'root') {
        return;
    }

    event.dataTransfer.setData('application/json', JSON.stringify({ type, id }));
    event.dataTransfer.effectAllowed = 'move';
}

function handleDragOver(event) {
    event.preventDefault();
    event.currentTarget.classList.add('drag-over');
}

function handleDragLeave(event) {
    event.currentTarget.classList.remove('drag-over');
}

function parseDragData(event) {
    const raw = event.dataTransfer.getData('application/json');
    if (!raw) {
        return null;
    }

    try {
        return JSON.parse(raw);
    } catch (error) {
        console.error('拖拽数据解析失败:', error);
        return null;
    }
}

function handleDropOnGroup(event, groupId) {
    event.preventDefault();
    event.currentTarget.classList.remove('drag-over');

    const payload = parseDragData(event);
    if (!payload) {
        return;
    }

    if (payload.type === 'note') {
        moveNoteToGroup(payload.id, groupId);
    }
    if (payload.type === 'group') {
        moveGroupToGroup(payload.id, groupId);
    }
}

function handleDropOnRoot(event) {
    event.preventDefault();
    event.currentTarget.classList.remove('drag-over');

    const payload = parseDragData(event);
    if (!payload) {
        return;
    }

    if (payload.type === 'note') {
        moveNoteToGroup(payload.id, null);
    }
    if (payload.type === 'group') {
        moveGroupToGroup(payload.id, null);
    }
}

async function moveNoteToGroup(noteId, groupId) {
    const note = findNoteById(noteId);
    if (!note || note.groupId === groupId) {
        return;
    }

    try {
        const updated = await noteAPI.updateGroup(noteId, groupId);
        updateNoteCollections(noteId, (item) => ({ ...item, groupId: updated.groupId }));

        if (currentNote && currentNote.id === noteId) {
            currentNote = { ...currentNote, groupId: updated.groupId };
            selectedGroupId = updated.groupId ?? null;
        }

        if (groupId) {
            ensureGroupVisible(groupId);
        }

        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
    } catch (error) {
        console.error('移动文档失败:', error);
        await DialogUtils.error(`移动失败: ${error.message}`);
    }
}

async function moveGroupToGroup(groupId, parentId) {
    const group = findGroupById(groupId);
    if (!group || group.id === parentId || group.parentId === parentId) {
        return;
    }

    try {
        const updated = await groupAPI.move(groupId, parentId);
        updateGroupCollections(groupId, (item) => ({ ...item, parentId: updated.parentId }));

        if (parentId) {
            ensureGroupVisible(parentId);
        }

        renderNotesTree();
        if (typeof refreshWorkspaceContext === 'function') {
            refreshWorkspaceContext();
        }
    } catch (error) {
        console.error('移动分组失败:', error);
        await DialogUtils.error(`移动失败: ${error.message}`);
    }
}

function toggleActionMenu(type, id, menuType = 'more') {
    const suffix = menuType ? `-${menuType}` : '';
    const menu = document.getElementById(`${type}-${id}${suffix}-menu`);
    if (!menu) {
        return;
    }

    const isOpen = menu.classList.contains('open');
    closeAllMenus();
    if (!isOpen) {
        menu.classList.add('open');
    }
}

function closeAllMenus() {
    document.querySelectorAll('.action-menu.open').forEach((menu) => {
        menu.classList.remove('open');
    });
}
