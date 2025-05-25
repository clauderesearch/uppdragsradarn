# KonsultRadarn Frontend

The frontend for konsultradarn built with Nuxt 3, Vue.js, and Tailwind CSS.

## Features

- Modern UI built with Tailwind CSS
- State management with Pinia
- Authentication with AWS Cognito
- Responsive design for mobile and desktop
- Advanced search capabilities
- Dashboard for tracking applications

## Setup

Make sure to install the dependencies:

```bash
# npm
npm install

# pnpm
pnpm install

# yarn
yarn install
```

## Development Server

Start the development server on `http://localhost:3000`:

```bash
# npm
npm run dev

# pnpm
pnpm run dev

# yarn
yarn dev
```

## Environment Variables

Create a `.env` file in the root of the frontend directory with the following variables:

```
NUXT_PUBLIC_API_BASE=http://localhost/api
NUXT_PUBLIC_COGNITO_REGION=your-cognito-region
NUXT_PUBLIC_COGNITO_USER_POOL_ID=your-cognito-user-pool-id
NUXT_PUBLIC_COGNITO_CLIENT_ID=your-cognito-client-id
```

## Production

Build the application for production:

```bash
# npm
npm run build

# pnpm
pnpm run build

# yarn
yarn build
```

Locally preview production build:

```bash
# npm
npm run preview

# pnpm
pnpm run preview

# yarn
yarn preview
```

Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.