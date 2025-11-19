// Conventional Commits rules for this repository
// Types and scopes align with docs/plan/commit-standards_plan.md
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'refactor', 'docs', 'chore', 'test', 'perf', 'style'
    ]],
    'scope-case': [2, 'always', ['kebab-case', 'lower-case']],
    'subject-case': [2, 'never', ['sentence-case', 'start-case', 'pascal-case', 'upper-case']],
    'subject-full-stop': [2, 'never', '.'],
    'header-max-length': [2, 'always', 72]
  }
}

