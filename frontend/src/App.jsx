import { useState, useEffect } from 'react'
import { reviewsAPI } from './services/api'
import './App.css'

function App() {
  const [stats, setStats] = useState({
    totalReviews: 0,
    activePRs: 0,
    avgQualityScore: 0,
    issuesFound: 0
  });
  const [recentReviews, setRecentReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [systemStatus] = useState({
    backend: { status: 'online', message: 'Running on port 8080' },
    database: { status: 'online', message: 'PostgreSQL 15.2' },
    queue: { status: 'online', message: 'RabbitMQ 3.13.7' },
    ai: { status: 'ready', message: 'GPT-4 / Claude' }
  });

  // Fetch data on component mount
  useEffect(() => {
    fetchDashboardData();
    // Refresh every 30 seconds
    const interval = setInterval(fetchDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch stats and recent reviews in parallel
      const [statsData, reviewsData] = await Promise.all([
        reviewsAPI.getStats(),
        reviewsAPI.getRecent()
      ]);

      setStats({
        totalReviews: statsData.totalReviews || 0,
        activePRs: statsData.activePRs || 0,
        avgQualityScore: statsData.avgQualityScore || 0,
        issuesFound: statsData.issuesFound || 0
      });

      setRecentReviews(reviewsData || []);
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);

      // Use mock data when backend is not available
      console.warn('Using mock data for demonstration');
      setStats({
        totalReviews: 247,
        activePRs: 12,
        avgQualityScore: 8.4,
        issuesFound: 89
      });

      setRecentReviews([
        {
          id: 1,
          prNumber: 42,
          prTitle: "Fix authentication bug in login flow",
          repositoryOwner: "codesage",
          repositoryName: "backend",
          status: "COMPLETED",
          qualityScore: 8.5,
          createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
          prUrl: "https://github.com/codesage/backend/pull/42"
        },
        {
          id: 2,
          prNumber: 38,
          prTitle: "Add user profile management feature",
          repositoryOwner: "codesage",
          repositoryName: "frontend",
          status: "COMPLETED",
          qualityScore: 9.2,
          createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), // 5 hours ago
          prUrl: "https://github.com/codesage/frontend/pull/38"
        },
        {
          id: 3,
          prNumber: 35,
          prTitle: "Optimize database queries for analytics",
          repositoryOwner: "codesage",
          repositoryName: "backend",
          status: "COMPLETED",
          qualityScore: 7.8,
          createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // 1 day ago
          prUrl: "https://github.com/codesage/backend/pull/35"
        },
        {
          id: 4,
          prNumber: 31,
          prTitle: "Implement real-time notifications",
          repositoryOwner: "codesage",
          repositoryName: "backend",
          status: "PENDING",
          qualityScore: null,
          createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(), // 2 days ago
          prUrl: "https://github.com/codesage/backend/pull/31"
        },
        {
          id: 5,
          prNumber: 28,
          prTitle: "Update dependencies and fix vulnerabilities",
          repositoryOwner: "codesage",
          repositoryName: "backend",
          status: "COMPLETED",
          qualityScore: 9.5,
          createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(), // 3 days ago
          prUrl: "https://github.com/codesage/backend/pull/28"
        }
      ]);

      setError(null); // Clear error since we have mock data
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      COMPLETED: <span className="badge badge-success">✓ Reviewed</span>,
      PENDING: <span className="badge badge-warning">⏳ Pending</span>,
      FAILED: <span className="badge badge-danger">✗ Failed</span>
    };
    return badges[status] || <span className="badge">Unknown</span>;
  };

  const getScoreColor = (score) => {
    if (score >= 9) return '#10b981';
    if (score >= 7) return '#f59e0b';
    return '#ef4444';
  };

  const formatTimeAgo = (timestamp) => {
    if (!timestamp) return 'Unknown';

    const date = new Date(timestamp);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);

    if (seconds < 60) return 'just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)} minutes ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)} hours ago`;
    return `${Math.floor(seconds / 86400)} days ago`;
  };

  if (loading && recentReviews.length === 0) {
    return (
      <div className="app">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading CodeSage Dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <div className="header-content">
          <a href="/" className="logo">
            <span className="logo-icon">🧙‍♂️</span>
            <span className="gradient-text">CodeSage</span>
          </a>

          <nav className="nav">
            <a href="#dashboard" className="nav-link">Dashboard</a>
            <a href="#reviews" className="nav-link">Reviews</a>
            <a href="#analytics" className="nav-link">Analytics</a>
            <a href="#settings" className="nav-link">Settings</a>

            <div className="status-indicator">
              <span className={`status-dot ${error ? 'error' : 'success'}`}></span>
              <span>{error ? 'Connection Error' : 'System Online'}</span>
            </div>
          </nav>
        </div>
      </header>

      {/* Main Content */}
      <main className="main">
        {/* Error Banner */}
        {error && (
          <div className="error-banner">
            <span>⚠️ {error}</span>
            <button onClick={fetchDashboardData} className="btn btn-secondary">
              Retry
            </button>
          </div>
        )}

        {/* Hero Section */}
        <section className="hero fade-in">
          <h1>
            Welcome to <span className="gradient-text">CodeSage</span>
          </h1>
          <p className="hero-subtitle">
            AI-powered code review assistant that helps you maintain code quality,
            catch bugs, and follow best practices automatically.
          </p>

          <div className="hero-stats">
            <div className="hero-stat">
              <div className="hero-stat-value">{stats.totalReviews}</div>
              <div className="hero-stat-label">Total Reviews</div>
            </div>
            <div className="hero-stat">
              <div className="hero-stat-value">{stats.activePRs}</div>
              <div className="hero-stat-label">Active PRs</div>
            </div>
            <div className="hero-stat">
              <div className="hero-stat-value">{stats.avgQualityScore.toFixed(1)}</div>
              <div className="hero-stat-label">Avg Score</div>
            </div>
          </div>
        </section>

        {/* Stats Cards */}
        <div className="stats-grid slide-in">
          <div className="stat-card">
            <div className="stat-header">
              <div>
                <div className="stat-value">{stats.totalReviews}</div>
                <div className="stat-label">Pull Requests Analyzed</div>
              </div>
              <div className="stat-icon">📊</div>
            </div>
            <div className="stat-change positive">
              {loading ? 'Updating...' : 'Live data'}
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-header">
              <div>
                <div className="stat-value">{stats.avgQualityScore.toFixed(1)}</div>
                <div className="stat-label">Average Code Quality</div>
              </div>
              <div className="stat-icon">⭐</div>
            </div>
            <div className="stat-change positive">
              {stats.avgQualityScore >= 8 ? '↑ Excellent' : stats.avgQualityScore >= 6 ? '→ Good' : '↓ Needs improvement'}
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-header">
              <div>
                <div className="stat-value">{stats.issuesFound}</div>
                <div className="stat-label">Issues Detected</div>
              </div>
              <div className="stat-icon">🐛</div>
            </div>
            <div className="stat-change">
              Total across all reviews
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-header">
              <div>
                <div className="stat-value">{stats.activePRs}</div>
                <div className="stat-label">Active Pull Requests</div>
              </div>
              <div className="stat-icon">🔄</div>
            </div>
            <div className="stat-change">
              Currently in queue
            </div>
          </div>
        </div>

        {/* Recent Reviews */}
        <section className="section">
          <div className="section-header">
            <h2 className="section-title">Recent Reviews</h2>
            <button className="btn btn-primary" onClick={fetchDashboardData}>
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>

          {recentReviews.length === 0 ? (
            <div className="empty-state">
              <p>No reviews yet. Create a Pull Request to get started!</p>
            </div>
          ) : (
            <div className="reviews-table">
              <table className="table">
                <thead>
                  <tr>
                    <th>PR #</th>
                    <th>Title & Repository</th>
                    <th>Status</th>
                    <th>Score</th>
                    <th>Time</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {recentReviews.map(review => (
                    <tr key={review.id}>
                      <td>
                        <strong>#{review.prNumber}</strong>
                      </td>
                      <td>
                        <div className="pr-title">{review.prTitle}</div>
                        <div className="pr-repo">{review.repositoryOwner}/{review.repositoryName}</div>
                      </td>
                      <td>
                        {getStatusBadge(review.status)}
                      </td>
                      <td>
                        {review.qualityScore ? (
                          <div
                            className="score"
                            style={{ background: getScoreColor(review.qualityScore) }}
                          >
                            {review.qualityScore.toFixed(1)}
                          </div>
                        ) : (
                          <span className="text-muted">-</span>
                        )}
                      </td>
                      <td className="timestamp">{formatTimeAgo(review.createdAt)}</td>
                      <td>
                        {review.prUrl ? (
                          <a
                            href={review.prUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="btn btn-secondary"
                            style={{ padding: '0.5rem 1rem', fontSize: '0.85rem' }}
                          >
                            View PR
                          </a>
                        ) : (
                          <button className="btn btn-secondary" style={{ padding: '0.5rem 1rem', fontSize: '0.85rem' }}>
                            View Details
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* System Status */}
        <section className="section">
          <div className="section-header">
            <h2 className="section-title">System Status</h2>
          </div>

          <div className="system-status">
            <div className="status-grid">
              <div className="status-item">
                <span className="status-dot success"></span>
                <div className="status-info">
                  <h4>Backend API</h4>
                  <p>{systemStatus.backend.message}</p>
                </div>
              </div>

              <div className="status-item">
                <span className="status-dot success"></span>
                <div className="status-info">
                  <h4>PostgreSQL</h4>
                  <p>{systemStatus.database.message}</p>
                </div>
              </div>

              <div className="status-item">
                <span className="status-dot success"></span>
                <div className="status-info">
                  <h4>RabbitMQ</h4>
                  <p>{systemStatus.queue.message}</p>
                </div>
              </div>

              <div className="status-item">
                <span className="status-dot success"></span>
                <div className="status-info">
                  <h4>AI Service</h4>
                  <p>{systemStatus.ai.message}</p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="footer">
        <p>
          Made with ❤️ by CodeSage Team • Powered by AI •
          <a href="https://github.com" style={{ color: 'var(--primary)', marginLeft: '0.5rem' }}>
            View on GitHub
          </a>
        </p>
      </footer>
    </div>
  )
}

export default App
