# Strict Workspace & Project Isolation Policy

## 1. Workspace Boundaries & File Operations
- **Scope Restriction**: All file operations, including reading (`view_file`), writing (`write_to_file`), editing (`replace_file_content`, `multi_replace_file_content`), searching (`grep_search`), and directory listing (`list_dir`), must strictly target paths inside this workspace directory.
- **Ignore External Files & Tabs**: Disregard any open editor tabs, file paths, or references that belong to directories outside this workspace root. Do not incorporate their content or structure into this project.

## 2. Shell Command Safety & Execution
- **Strict Working Directory**: All terminal commands (`run_command`) must run with `Cwd` strictly set to this workspace root.
- **No Path Traversal**: Do not execute commands that inspect, navigate to, or modify parent or sibling directories (e.g., avoid `cd ..`, `ls ../`, or referencing paths outside this workspace).
- **Process Isolation**: Do not access or interact with background processes, services, or ports originating from other projects or folders.

## 3. Chat, Context & Memory Isolation
- **Block Conversation Summaries & History**: Do NOT read, list, summarize, or disclose any past conversation titles, session IDs, or metadata (including `<conversation_summaries>` injected by the IDE). If asked about other chats or past sessions, strictly state that cross-session memory is disabled.
- **No Cross-Conversation Contamination**: Do not access, read, or search conversation transcripts (`<appDataDir>/brain/*`), artifacts, or knowledge items belonging to other conversation IDs or other projects.
- **Independent Context**: Treat this workspace and all requests in it as completely standalone. Never assume dependencies, configurations, or coding patterns from other active or previous projects.

## 4. Artifact & Output Containment
- All generated code, temporary files, scratch scripts, and project assets must remain strictly inside this workspace.
