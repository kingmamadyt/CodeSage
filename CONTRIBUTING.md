# Contributing to CodeSage

Thank you for your interest in contributing to CodeSage! This document provides guidelines for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/albonidrizi/CodeSage/issues)
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version, etc.)

### Suggesting Features

1. Check if the feature has been suggested in [Issues](https://github.com/albonidrizi/CodeSage/issues)
2. Create a new issue with:
   - Clear use case
   - Proposed solution
   - Alternative approaches considered

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Make your changes**:
   - Follow existing code style
   - Add tests for new functionality
   - Update documentation as needed
4. **Commit your changes**: `git commit -m 'feat: add amazing feature'`
   - Use [Conventional Commits](https://www.conventionalcommits.org/)
5. **Push to your fork**: `git push origin feature/your-feature-name`
6. **Open a Pull Request**

### Commit Message Format

Use Conventional Commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(ai): add Claude 3 Opus support
fix(webhook): handle missing PR metadata
docs(readme): update installation instructions
```

## Development Setup

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.9+

### Local Development

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/CodeSage.git
cd CodeSage

# Start infrastructure
docker-compose up -d postgres rabbitmq

# Backend
cd backend
mvn spring-boot:run

# Frontend (in another terminal)
cd frontend
npm install
npm run dev
```

### Running Tests

```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
npm test
```

## Code Style

### Java
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Lombok for boilerplate reduction
- Add JavaDoc for public APIs

### JavaScript/React
- Use ESLint with Airbnb config
- Use functional components with hooks
- Add JSDoc for complex functions

### General
- Keep functions small and focused
- Write self-documenting code
- Add comments for complex logic only

## Testing Guidelines

- Write tests for all new features
- Maintain or improve code coverage
- Use meaningful test names
- Test edge cases and error scenarios

## Documentation

- Update README.md for user-facing changes
- Update docs/ for detailed guides
- Add inline comments for complex code
- Update API documentation

## Questions?

Feel free to:
- Open an issue for discussion
- Reach out on [LinkedIn](https://linkedin.com/in/albonidrizi)
- Email: albonidrizi@gmail.com

Thank you for contributing! 🎉
