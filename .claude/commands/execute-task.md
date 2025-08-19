# Execute Task

Execute implementation tasks from `./.claude/tasks/` directory with adaptive planning and iterative execution.
Execute a just task which is in $ARGUMENTS.

## Usage

```bash
claude execute-task <task-name>
```

**Arguments:**
- `<task-name>` (required): Name of the task file in `./.claude/tasks/` directory (without .md extension)

**Examples:**
```bash
claude execute-task mcp-server-implementation-plan
claude execute-task api-refactoring-plan
claude execute-task performance-optimization
```

## Command Description

This command executes task plans from the `.claude/tasks/` directory as a **Senior Software Engineer** proficient in Java, Axon Framework 4, JUnit 5, and AssertJ. The command treats the plan as **inspiration** and adapts it during execution as needed.

### Execution Approach

1. **📋 Plan Analysis**: Read and understand the task plan from the specified file
2. **🎯 Adaptive Planning**: Plan 3 steps ahead based on current context and progress
3. **⚡ Execute**: Implement the 3 planned steps with full code implementation
4. **✅ Review Checkpoint**: Present completed work and ask for human review/adjustments
5. **🔄 Iterate**: Plan next 3 steps based on progress and feedback, then repeat

### Key Behaviors

- **Plan as Inspiration**: Use the task plan as guidance, but adapt based on:
  - Current codebase state
  - Discovered technical constraints
  - Better implementation approaches found during execution
  - Human feedback and preferences

- **Progress Tracking**: 
  - Mark completed checkboxes in the plan file
  - Update plan with discovered requirements or changes
  - Document decisions and rationale for deviations

- **Senior Engineer Approach**:
  - Ask clarifying questions when context is unclear
  - Suggest improvements and alternatives
  - Follow best practices for Java, Axon Framework 4, JUnit 5, AssertJ
  - Ensure code quality, testing, and maintainability
  - Respect existing codebase patterns and conventions

- **Human-in-the-Loop**:
  - Present completed work after every 3 steps
  - Ask for feedback on implementation approach
  - Confirm direction before proceeding with next steps
  - Suggest plan adjustments when needed

### Example Execution Flow

```
📋 Reading plan from: ./.claude/tasks/mcp-server-implementation-plan.md
🎯 Planning next 3 steps:
   1. Add Spring AI MCP Server dependencies to pom.xml
   2. Create base MCP configuration in shared/mcp/configuration/
   3. Set up authentication/authorization structure

⚡ Executing steps 1-3...
   ✅ Step 1: Added spring-ai-starter-mcp-server-webmvc dependency
   ✅ Step 2: Created McpServerConfiguration.java with base setup
   ✅ Step 3: Implemented McpSecurityConfig.java with authentication

📊 Progress Update: Updated plan file - marked 3 items as completed

🤔 Review needed: I've completed the foundation setup. The MCP server is now configured with basic authentication. I noticed the existing codebase uses custom GameMetaData for request context - should we integrate this with MCP authentication?

Next planned steps:
   4. Create ModelContextProtocol.java in creaturerecruitment/write/builddwelling/
   5. Implement build dwelling tool using existing BuildDwelling command
   6. Add JSON schema generation for BuildDwelling parameters

👤 [Awaiting human feedback before proceeding...]
```

### Questions to Ask

The command will proactively ask questions like:
- "I see the codebase uses [pattern X]. Should I follow the same pattern for MCP implementation?"
- "The existing authentication uses [approach Y]. How should this integrate with MCP security?"
- "I found [technical constraint Z]. Should we adjust the plan to accommodate this?"
- "Would you prefer [implementation option A] or [implementation option B] for [specific feature]?"

### Error Handling

- If the specified task file doesn't exist, list available files in `./.claude/tasks/`
- If a step fails, ask for guidance before proceeding
- If dependencies are missing, suggest adding them and ask for confirmation
- If tests fail, fix them before marking steps as complete

### Plan Updates

The command will update the original plan file with:
- ✅ Completed checkboxes
- 📝 Notes about implementation decisions
- 🔄 Adjusted steps based on discoveries
- 📅 Timestamps for major milestones

This ensures the plan remains a living document that reflects actual implementation progress and decisions.