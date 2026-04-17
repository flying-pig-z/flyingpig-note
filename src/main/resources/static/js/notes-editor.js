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
                toolbar: ['both', 'code-theme', 'content-theme', 'export', 'outline', 'preview', 'devtools', 'info', 'help']
            }
        ],
        toolbarConfig: { pin: true },
        counter: { enable: true, type: 'text' },
        cache: { enable: false },
        preview: {
            maxWidth: 9999,
            markdown: { toc: true, mark: true, footnotes: true, autoSpace: true },
            math: { engine: 'KaTeX' },
            hljs: { lineNumber: true, style: 'github' }
        },
        placeholder: '在这里继续写文档内容...',
        input: () => {
            if (currentNote) {
                triggerAutoSave();
            }
        },
        after: () => {
            if (initialContent) {
                vditor.setValue(initialContent);
            }
        },
        upload: {
            accept: 'image/*',
            handler: (files) => {
                for (const file of files) {
                    const reader = new FileReader();
                    reader.onload = (event) => {
                        const base64 = event.target.result;
                        vditor.insertValue(`![${file.name}](${base64})`);
                    };
                    reader.readAsDataURL(file);
                }
                return null;
            }
        },
        outline: { enable: false, position: 'left' },
        lang: 'zh_CN'
    });
}

function getDraftTitle() {
    if (!currentNote) {
        return '';
    }

    const draftTitle = elements.noteTitleInput.value.trim();
    return draftTitle || currentNote.title || '未命名文档';
}

function syncActiveNoteTitlePreview(title) {
    if (!currentNote) {
        return;
    }

    const target = document.querySelector(`.note-row[data-note-id="${currentNote.id}"] .note-title`);
    if (target) {
        target.textContent = title || '未命名文档';
    }
}

function renderEditorHeader(note) {
    const title = note.title || '未命名文档';

    if (elements.noteTitleInput.value !== title) {
        elements.noteTitleInput.value = title;
    }

    elements.editorMeta.innerHTML = `
        <span><i class="fas fa-clock"></i>最后更新 ${escapeHtml(formatRelativeTime(note.updateTime))}</span>
    `;
}

function showNoteEditor(note) {
    if (typeof toggleWorkspacePanels === 'function') {
        toggleWorkspacePanels('editor');
    }

    renderEditorHeader(note);

    if (!vditor) {
        initVditor(note.content || '');
    } else if (getEditorContent() !== (note.content || '')) {
        setEditorContent(note.content || '');
    }
}

function hasUnsavedChanges() {
    if (!currentNote) {
        return false;
    }

    return getDraftTitle() !== (currentNote.title || '')
        || getEditorContent() !== (currentNote.content || '');
}

async function saveCurrentNote() {
    if (!currentNote) {
        return;
    }

    const title = getDraftTitle();
    const content = getEditorContent();

    try {
        const updatedNote = await noteAPI.update(
            currentNote.id,
            title,
            content,
            kbId,
            currentNote.groupId
        );

        updateNoteCollections(currentNote.id, () => updatedNote);
        currentNote = updatedNote;
        renderEditorHeader(updatedNote);
        renderNotesTree();
    } catch (error) {
        console.error('保存文档失败:', error);
        await DialogUtils.error(`保存失败: ${error.message}`, '保存失败');
        throw error;
    }
}

async function selectNote(noteId) {
    const requestId = ++selectNoteRequestId;

    try {
        if (currentNote && hasUnsavedChanges()) {
            SaveIndicator.show('正在保存...', 'saving');
            await saveCurrentNote();
            SaveIndicator.show('已保存', 'success');
        }

        const fullNote = await noteAPI.getById(noteId);
        if (requestId !== selectNoteRequestId) {
            return;
        }

        currentNote = fullNote;
        selectedGroupId = fullNote.groupId ?? null;
        ensureGroupVisible(selectedGroupId);

        renderNotesTree();
        showNoteEditor(fullNote);
    } catch (error) {
        if (requestId !== selectNoteRequestId) {
            return;
        }
        console.error('打开文档失败:', error);
        SaveIndicator.show('加载失败', 'error');
        await DialogUtils.error(`加载文档失败: ${error.message}`);
    }
}

function triggerAutoSave() {
    if (!currentNote || !hasUnsavedChanges()) {
        return;
    }

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
    }, 1200);
}

function setupAutoSave() {
    if (!elements.noteTitleInput) {
        return;
    }
}

function setupTitleInputAutoSave() {
    if (!elements.noteTitleInput) {
        return;
    }

    elements.noteTitleInput.addEventListener('input', () => {
        if (!currentNote) {
            return;
        }

        const title = elements.noteTitleInput.value.trim() || '未命名文档';
        syncActiveNoteTitlePreview(title);
        triggerAutoSave();
    });

    elements.noteTitleInput.addEventListener('blur', () => {
        if (!currentNote) {
            return;
        }

        if (!elements.noteTitleInput.value.trim()) {
            elements.noteTitleInput.value = currentNote.title || '未命名文档';
            syncActiveNoteTitlePreview(elements.noteTitleInput.value);
            renderEditorHeader(currentNote);
        }
    });
}
