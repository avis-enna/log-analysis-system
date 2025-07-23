from flask import Flask, render_template, request, jsonify, send_file
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from datetime import datetime, timedelta
import os
import json
import pandas as pd
import plotly.graph_objs as go
import plotly.utils
from dotenv import load_dotenv
from log_processor import LogProcessor
from alert_manager import AlertManager
from database import init_db
from models import LogEntry, Alert, Dashboard
import threading
import time

# Load environment variables
load_dotenv()

app = Flask(__name__)
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', 'dev-secret-key')
app.config['SQLALCHEMY_DATABASE_URI'] = os.getenv('DATABASE_URL', 'sqlite:///log_analysis.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['UPLOAD_FOLDER'] = os.getenv('UPLOAD_FOLDER', 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024  # 50MB max file size

db = SQLAlchemy(app)
CORS(app)

# Initialize components
log_processor = LogProcessor()
alert_manager = AlertManager()

# Ensure upload directory exists
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

@app.route('/')
def dashboard():
    """Main dashboard view"""
    return render_template('dashboard.html')

@app.route('/api/stats')
def get_stats():
    """Get overall statistics"""
    try:
        # Get recent log entries
        recent_logs = LogEntry.query.filter(
            LogEntry.timestamp >= datetime.now() - timedelta(hours=24)
        ).all()
        
        # Calculate statistics
        total_logs = len(recent_logs)
        error_logs = len([log for log in recent_logs if log.level == 'ERROR'])
        warning_logs = len([log for log in recent_logs if log.level == 'WARNING'])
        info_logs = len([log for log in recent_logs if log.level == 'INFO'])
        
        # Get unique sources
        sources = list(set([log.source for log in recent_logs]))
        
        # Get recent alerts
        recent_alerts = Alert.query.filter(
            Alert.created_at >= datetime.now() - timedelta(hours=24)
        ).count()
        
        stats = {
            'total_logs': total_logs,
            'error_logs': error_logs,
            'warning_logs': warning_logs,
            'info_logs': info_logs,
            'sources': sources,
            'recent_alerts': recent_alerts,
            'uptime': '24h 15m',  # Mock uptime
            'processing_rate': f"{total_logs}/hour"
        }
        
        return jsonify(stats)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/logs')
def get_logs():
    """Get paginated log entries"""
    try:
        page = request.args.get('page', 1, type=int)
        per_page = request.args.get('per_page', 50, type=int)
        level = request.args.get('level', '')
        source = request.args.get('source', '')
        
        # Build query
        query = LogEntry.query
        
        if level:
            query = query.filter(LogEntry.level == level)
        if source:
            query = query.filter(LogEntry.source == source)
        
        # Order by timestamp descending
        query = query.order_by(LogEntry.timestamp.desc())
        
        # Paginate
        logs = query.paginate(
            page=page, per_page=per_page, error_out=False
        )
        
        # Convert to JSON
        log_data = []
        for log in logs.items:
            log_data.append({
                'id': log.id,
                'timestamp': log.timestamp.isoformat(),
                'level': log.level,
                'source': log.source,
                'message': log.message,
                'ip_address': log.ip_address,
                'user_agent': log.user_agent
            })
        
        return jsonify({
            'logs': log_data,
            'pagination': {
                'page': logs.page,
                'per_page': logs.per_page,
                'total': logs.total,
                'pages': logs.pages
            }
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/upload', methods=['POST'])
def upload_log_file():
    """Upload and process log file"""
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'No file provided'}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({'error': 'No file selected'}), 400
        
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            file.save(filepath)
            
            # Process the file
            results = log_processor.process_file(filepath)
            
            return jsonify({
                'message': 'File uploaded and processed successfully',
                'results': results
            })
        else:
            return jsonify({'error': 'Invalid file type'}), 400
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/charts/timeline')
def get_timeline_chart():
    """Get timeline chart data"""
    try:
        hours = request.args.get('hours', 24, type=int)
        
        # Get logs from the last N hours
        since = datetime.now() - timedelta(hours=hours)
        logs = LogEntry.query.filter(LogEntry.timestamp >= since).all()
        
        # Group by hour and level
        df = pd.DataFrame([{
            'timestamp': log.timestamp,
            'level': log.level
        } for log in logs])
        
        if not df.empty:
            df['hour'] = df['timestamp'].dt.floor('h')
            grouped = df.groupby(['hour', 'level']).size().reset_index(name='count')
            
            # Create plotly chart
            fig = go.Figure()
            
            for level in ['ERROR', 'WARNING', 'INFO']:
                level_data = grouped[grouped['level'] == level]
                fig.add_trace(go.Scatter(
                    x=level_data['hour'],
                    y=level_data['count'],
                    mode='lines+markers',
                    name=level,
                    line=dict(width=2)
                ))
            
            fig.update_layout(
                title='Log Activity Timeline',
                xaxis_title='Time',
                yaxis_title='Count',
                hovermode='x unified'
            )
            
            graphJSON = json.dumps(fig, cls=plotly.utils.PlotlyJSONEncoder)
            return jsonify({'chart': graphJSON})
        else:
            return jsonify({'chart': None})
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/charts/sources')
def get_sources_chart():
    """Get sources distribution chart"""
    try:
        # Get logs from the last 24 hours
        since = datetime.now() - timedelta(hours=24)
        logs = LogEntry.query.filter(LogEntry.timestamp >= since).all()
        
        # Count by source
        source_counts = {}
        for log in logs:
            source_counts[log.source] = source_counts.get(log.source, 0) + 1
        
        if source_counts:
            fig = go.Figure(data=[go.Pie(
                labels=list(source_counts.keys()),
                values=list(source_counts.values()),
                hole=0.3
            )])
            
            fig.update_layout(title='Log Sources Distribution')
            
            graphJSON = json.dumps(fig, cls=plotly.utils.PlotlyJSONEncoder)
            return jsonify({'chart': graphJSON})
        else:
            return jsonify({'chart': None})
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/alerts')
def get_alerts():
    """Get recent alerts"""
    try:
        alerts = Alert.query.order_by(Alert.created_at.desc()).limit(10).all()
        
        alert_data = []
        for alert in alerts:
            alert_data.append({
                'id': alert.id,
                'type': alert.type,
                'severity': alert.severity,
                'message': alert.message,
                'created_at': alert.created_at.isoformat(),
                'resolved': alert.resolved
            })
        
        return jsonify({'alerts': alert_data})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/search')
def search_logs():
    """Search logs by query"""
    try:
        query = request.args.get('q', '')
        if not query:
            return jsonify({'error': 'No search query provided'}), 400
        
        # Search in message field
        logs = LogEntry.query.filter(
            LogEntry.message.contains(query)
        ).order_by(LogEntry.timestamp.desc()).limit(100).all()
        
        log_data = []
        for log in logs:
            log_data.append({
                'id': log.id,
                'timestamp': log.timestamp.isoformat(),
                'level': log.level,
                'source': log.source,
                'message': log.message,
                'ip_address': log.ip_address
            })
        
        return jsonify({'logs': log_data})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

def allowed_file(filename):
    """Check if file extension is allowed"""
    ALLOWED_EXTENSIONS = {'log', 'txt', 'json'}
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def secure_filename(filename):
    """Secure filename by removing dangerous characters"""
    import re
    filename = re.sub(r'[^\w\s-]', '', filename).strip()
    return re.sub(r'[-\s]+', '-', filename)

def background_processor():
    """Background task to process logs continuously"""
    while True:
        try:
            # Process any new log files
            log_processor.process_directory(app.config['UPLOAD_FOLDER'])
            
            # Check for alerts
            alert_manager.check_alerts()
            
            # Sleep for processing interval
            time.sleep(int(os.getenv('PROCESSING_INTERVAL', 30)))
        except Exception as e:
            print(f"Background processing error: {e}")
            time.sleep(60)  # Wait longer on error

if __name__ == '__main__':
    # Initialize database
    with app.app_context():
        init_db()
    
    # Start background processor
    bg_thread = threading.Thread(target=background_processor, daemon=True)
    bg_thread.start()
    
    # Run the app
    app.run(debug=os.getenv('DEBUG', 'True').lower() == 'true', host='0.0.0.0', port=5000)
