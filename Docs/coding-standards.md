# EthioStat Coding Standards

This document establishes comprehensive coding standards and conventions for the EthioStat project to ensure consistent, maintainable, and high-quality code across the development team.

## File Organization Rules

### Root Directory Policy
- **Only README.md allowed in root directory**
- All other documentation must be placed in the `Docs/` directory
- Configuration files (package.json, tsconfig.json, etc.) remain in root as required by tooling
- Build and deployment scripts belong in `scripts/` directory

### Documentation Structure
- **Consolidate related documentation** into single files when functionality overlaps
- **Merge implementation guides** with architectural documentation where appropriate
- **Maintain clear separation** between user documentation and technical specifications

### Directory Structure Standards
```
src/
├── constants/          # Application constants and configuration
├── data/              # Data services, persistence, and external integrations
├── domain/            # Business logic, types, and use cases
├── lib/               # Utility functions and shared libraries
├── components/        # Reusable UI components
├── screens/           # Screen-level components
└── store/             # State management
```

## TypeScript Standards

### Language Requirements
- **Use TypeScript for all code** - no JavaScript files allowed
- **Prefer interfaces over types** for object definitions
- **Avoid enums** - use const objects or maps instead
- **Use functional components** with TypeScript interfaces for React

### Type Definitions
```typescript
// ✅ Good - Interface
interface UserData {
  id: string;
  name: string;
  isActive: boolean;
}

// ❌ Avoid - Type alias for objects
type UserData = {
  id: string;
  name: string;
  isActive: boolean;
}

// ✅ Good - Const object instead of enum
const Status = {
  PENDING: 'pending',
  COMPLETED: 'completed',
  FAILED: 'failed'
} as const;

// ❌ Avoid - Enum
enum Status {
  PENDING = 'pending',
  COMPLETED = 'completed',
  FAILED = 'failed'
}
```

## Code Style and Structure

### Programming Patterns
- **Use functional and declarative programming patterns**
- **Avoid classes** - prefer functions and hooks
- **Prefer iteration and modularization** over code duplication
- **Write concise, technical code** with accurate examples

### Variable Naming
- **Use descriptive variable names** with auxiliary verbs
- **Boolean variables** should use `is`, `has`, `can`, `should` prefixes
- **Event handlers** should use `handle` or `on` prefixes

```typescript
// ✅ Good
const isLoading = true;
const hasError = false;
const canSubmit = form.isValid;
const handleSubmit = () => { /* ... */ };

// ❌ Avoid
const loading = true;
const error = false;
const submit = form.isValid;
const onSubmit = () => { /* ... */ };
```

### File Structure
Organize file contents in this order:
1. **Exported component** (main component)
2. **Subcomponents** (internal components)
3. **Helper functions** (utilities specific to this file)
4. **Static content** (constants, default values)
5. **Type definitions** (interfaces, types)

## Naming Conventions

### Directory Naming
- **Use lowercase with dashes** for directories
- **Be descriptive and consistent**

```
// ✅ Good
components/auth-wizard/
screens/transaction-history/
utils/date-helpers/

// ❌ Avoid
components/AuthWizard/
screens/transactionHistory/
utils/dateHelpers/
```

### Component Exports
- **Favor named exports** for components
- **Use default exports sparingly** - only for main entry points

```typescript
// ✅ Good
export function AuthWizard() { /* ... */ }
export function LoginForm() { /* ... */ }

// ❌ Avoid (unless main entry point)
export default function AuthWizard() { /* ... */ }
```

## Syntax and Formatting

### Function Declarations
- **Use the "function" keyword** for pure functions
- **Use arrow functions** for callbacks and inline functions
- **Avoid unnecessary curly braces** in conditionals

```typescript
// ✅ Good - Pure function
function calculateBalance(transactions: Transaction[]): number {
  return transactions.reduce((sum, t) => sum + t.amount, 0);
}

// ✅ Good - Callback
const handleClick = () => setIsOpen(true);

// ✅ Good - Concise conditional
if (isLoading) return <Spinner />;

// ❌ Avoid - Unnecessary braces
if (isLoading) {
  return <Spinner />;
}
```

### JSX Standards
- **Use declarative JSX** patterns
- **Prefer explicit boolean props**
- **Use fragments when needed**

```typescript
// ✅ Good
<Button disabled={isLoading} variant="primary">
  Submit
</Button>

// ❌ Avoid
<Button disabled={isLoading ? true : false} variant="primary">
  Submit
</Button>
```

## React and Performance Standards

### Component Architecture
- **Use functional components** with TypeScript interfaces
- **Minimize 'use client' usage** - favor React Server Components (RSC)
- **Wrap client components in Suspense** with fallback
- **Use dynamic loading** for non-critical components

### Performance Optimization
- **Minimize 'useEffect' and 'setState'** usage
- **Favor server-side rendering** when possible
- **Implement lazy loading** for heavy components
- **Optimize images**: WebP format, include size data, implement lazy loading

```typescript
// ✅ Good - Server Component
function TransactionList({ transactions }: { transactions: Transaction[] }) {
  return (
    <div>
      {transactions.map(transaction => (
        <TransactionCard key={transaction.id} transaction={transaction} />
      ))}
    </div>
  );
}

// ✅ Good - Client Component with Suspense
'use client';
import { Suspense } from 'react';

function InteractiveChart() {
  return (
    <Suspense fallback={<ChartSkeleton />}>
      <Chart />
    </Suspense>
  );
}
```

## UI and Styling Standards

### UI Framework Requirements
- **Use Shadcn UI** for component library
- **Use Radix** for headless UI primitives
- **Use Tailwind CSS** for styling
- **Implement responsive design** with mobile-first approach

### Styling Patterns
```typescript
// ✅ Good - Mobile-first responsive design
<div className="w-full p-4 md:p-6 lg:p-8">
  <h1 className="text-lg md:text-xl lg:text-2xl font-bold">
    Transaction History
  </h1>
</div>

// ✅ Good - Consistent spacing and typography
<Card className="p-6 space-y-4">
  <CardHeader>
    <CardTitle className="text-xl font-semibold">Balance Overview</CardTitle>
  </CardHeader>
  <CardContent className="space-y-2">
    {/* Content */}
  </CardContent>
</Card>
```

## State Management and URL Handling

### State Management
- **Use 'nuqs' for URL search parameter state management**
- **Minimize global state** - prefer local state when possible
- **Use React Context** for shared state across components

### URL State Management
```typescript
// ✅ Good - Using nuqs for URL state
import { useQueryState } from 'nuqs';

function TransactionFilter() {
  const [filter, setFilter] = useQueryState('filter', { defaultValue: 'all' });
  const [dateRange, setDateRange] = useQueryState('dateRange');
  
  return (
    <FilterControls 
      filter={filter} 
      onFilterChange={setFilter}
      dateRange={dateRange}
      onDateRangeChange={setDateRange}
    />
  );
}
```

## Code Quality and Configuration

### No Hardcoded Values Rule
- **Move all configuration to .env files** or constants
- **Use environment variables** for API endpoints, feature flags
- **Create constants files** for application-specific values

```typescript
// ✅ Good - Environment configuration
const API_BASE_URL = process.env.VITE_API_BASE_URL || 'http://localhost:3000';
const FEATURE_FLAGS = {
  ENABLE_SMS_PARSING: process.env.VITE_ENABLE_SMS_PARSING === 'true',
  ENABLE_USSD: process.env.VITE_ENABLE_USSD === 'true'
};

// ✅ Good - Constants file
export const TRANSACTION_TYPES = {
  INCOME: 'income',
  EXPENSE: 'expense',
  TRANSFER: 'transfer'
} as const;

// ❌ Avoid - Hardcoded values
const apiUrl = 'http://localhost:3000'; // Should be in .env
const maxRetries = 3; // Should be in constants
```

### Performance Metrics
- **Optimize Web Vitals** (LCP, CLS, FID)
- **Monitor bundle size** and implement code splitting
- **Use React DevTools Profiler** for performance analysis

## Framework-Specific Guidelines

### Next.js Patterns (if applicable)
- **Follow Next.js documentation** for Data Fetching, Rendering, and Routing
- **Use App Router** for new features
- **Implement proper SEO** with metadata API

### Capacitor Integration
- **Use proper TypeScript types** for Capacitor plugins
- **Handle platform differences** gracefully
- **Implement proper error handling** for native features

## Code Review and Quality Assurance

### Before Committing
- [ ] All TypeScript errors resolved
- [ ] No hardcoded values present
- [ ] Components follow naming conventions
- [ ] Responsive design implemented
- [ ] Performance considerations addressed
- [ ] Documentation updated if needed

### Testing Standards
- **Write unit tests** for utility functions
- **Test component behavior** not implementation details
- **Mock external dependencies** properly
- **Maintain high test coverage** for critical paths

---

## Enforcement

These standards should be enforced through:
- **ESLint configuration** with TypeScript rules
- **Prettier configuration** for consistent formatting
- **Pre-commit hooks** to validate code quality
- **Code review process** to ensure adherence
- **Documentation updates** when standards evolve

This document serves as the authoritative source for all EthioStat development standards and should be referenced during development, code reviews, and onboarding of new team members.
