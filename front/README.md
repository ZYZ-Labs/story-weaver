# Story Weaver Frontend

This is the frontend application for Story Weaver, built with Vue 3, Vuetify 3, and Vite.

## Quick Start

### Prerequisites
- Node.js 18+ and npm

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start development server:
```bash
npm run dev
```

3. Build for production:
```bash
npm run build
```

## Project Structure

```
front/
├── src/
│   ├── assets/          # Static assets
│   ├── components/      # Vue components
│   ├── composables/     # Vue composables
│   ├── layouts/         # Layout components
│   ├── pages/          # Page components
│   ├── router/         # Vue Router configuration
│   ├── stores/         # Pinia stores
│   ├── styles/         # Global styles
│   ├── types/          # TypeScript types
│   └── utils/          # Utility functions
├── public/             # Public assets
└── package.json        # Dependencies
```

## Features

- **Authentication**: Login, registration, and JWT token management
- **Project Management**: Create, edit, and manage writing projects
- **Chapter Management**: Organize chapters within projects
- **Character Management**: Create and manage story characters
- **World Building**: Manage world settings and locations
- **AI Writing Assistance**: AI-powered writing suggestions
- **Causality Tracking**: Track cause-effect relationships in stories
- **Plot Management**: Organize and manage story plots

## Development

### Environment Variables

Create a `.env` file in the root directory:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=Story Weaver
```

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm run format` - Format code with Prettier

## API Integration

The frontend communicates with the backend API at `VITE_API_BASE_URL`. Key endpoints include:

- `POST /auth/login` - User authentication
- `GET /projects` - Get user projects
- `POST /projects` - Create new project
- `GET /chapters` - Get project chapters
- `POST /ai/writing` - AI writing assistance
- `GET /causalities` - Get causality relationships
- `POST /plots` - Create plot points

## Deployment

### Docker Deployment

Build and run with Docker:

```bash
# Build the image
docker build -t story-weaver-frontend .

# Run the container
docker run -p 3000:3000 story-weaver-frontend
```

### Docker Compose

Use the provided `docker-compose.yml` for full stack deployment:

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down
```

## Technologies Used

- **Vue 3** - Progressive JavaScript framework
- **Vuetify 3** - Material Design component framework
- **Vite** - Next generation frontend tooling
- **TypeScript** - Type-safe JavaScript
- **Pinia** - State management
- **Vue Router** - Client-side routing
- **Axios** - HTTP client
- **ESLint** - Code linting
- **Prettier** - Code formatting

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## License

MIT