"""
Modern AI Voice Interview System - Professional UI
"""
import sys
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

import streamlit as st

# Page configuration - must be first Streamlit command
st.set_page_config(
    page_title="AI Interview",
    page_icon="🤖",
    layout="wide",
    initial_sidebar_state="collapsed"
)

try:
    import sounddevice as sd
    import soundfile as sf
    AUDIO_AVAILABLE = True
except ImportError:
    AUDIO_AVAILABLE = False
    # Audio not available — silently disabled

import numpy as np
from datetime import datetime, timedelta
import time
import uuid

try:
    import cv2
    from PIL import Image
    VIDEO_AVAILABLE = True
except ImportError:
    VIDEO_AVAILABLE = True  # Browser-based face detection — no opencv needed
    cv2 = None

import io
import streamlit.components.v1 as components
from services import get_ai_service, get_stt_service, get_tts_service, get_audio_recorder
from database import SessionLocal
from models.database import Interview, InterviewSession, LegacyInterview
from config.settings import AUDIO_STORAGE_PATH, AUDIO_SAMPLE_RATE, AUDIO_MAX_DURATION

# Initialize session state
if 'interview_started' not in st.session_state:
    st.session_state.interview_started = False
if 'current_question' not in st.session_state:
    st.session_state.current_question = ""
if 'question_count' not in st.session_state:
    st.session_state.question_count = 0
if 'total_questions' not in st.session_state:
    st.session_state.total_questions = 8
if 'chat_history' not in st.session_state:
    st.session_state.chat_history = []
if 'is_recording' not in st.session_state:
    st.session_state.is_recording = False
if 'is_paused' not in st.session_state:
    st.session_state.is_paused = False
if 'start_time' not in st.session_state:
    st.session_state.start_time = None
if 'interview_duration' not in st.session_state:
    st.session_state.interview_duration = 600  # 10 minutes default
if 'notes' not in st.session_state:
    st.session_state.notes = []
if 'candidate_name' not in st.session_state:
    st.session_state.candidate_name = ""
if 'candidate_email' not in st.session_state:
    st.session_state.candidate_email = ""
if 'interviewer_id' not in st.session_state:
    st.session_state.interviewer_id = None
if 'job_role' not in st.session_state:
    st.session_state.job_role = ""
if 'previous_questions' not in st.session_state:
    st.session_state.previous_questions = []
if 'evaluations' not in st.session_state:
    st.session_state.evaluations = []
if 'answer_mode' not in st.session_state:
    st.session_state.answer_mode = 'voice'  # 'voice' or 'text'
if 'text_answer' not in st.session_state:
    st.session_state.text_answer = ""
if 'question_types' not in st.session_state:
    st.session_state.question_types = []  # Track which questions need text vs voice
if 'last_spoken_question' not in st.session_state:
    st.session_state.last_spoken_question = ""
if 'video_enabled' not in st.session_state:
    st.session_state.video_enabled = False
if 'cheating_warnings' not in st.session_state:
    st.session_state.cheating_warnings = []
if 'face_detection_enabled' not in st.session_state:
    st.session_state.face_detection_enabled = True
if 'video_frame' not in st.session_state:
    st.session_state.video_frame = None
if 'generating_question' not in st.session_state:
    st.session_state.generating_question = False
if 'last_video_refresh' not in st.session_state:
    st.session_state.last_video_refresh = 0
if 'question_ready' not in st.session_state:
    st.session_state.question_ready = False
if 'extracted_skills' not in st.session_state:
    st.session_state.extracted_skills = []
if 'difficulty_distribution' not in st.session_state:
    st.session_state.difficulty_distribution = {"basic": 30, "intermediate": 50, "advanced": 20}
if 'experience_level' not in st.session_state:
    st.session_state.experience_level = "mid"
if 'job_description' not in st.session_state:
    st.session_state.job_description = ""
if 'difficulty_level' not in st.session_state:
    st.session_state.difficulty_level = ""
if 'auto_start_interview' not in st.session_state:
    st.session_state.auto_start_interview = False
if 'voice_answer_ready' not in st.session_state:
    st.session_state.voice_answer_ready = False
if 'voice_bytes' not in st.session_state:
    st.session_state.voice_bytes = None
if 'exam_terminated' not in st.session_state:
    st.session_state.exam_terminated = False
if 'cheat_violations' not in st.session_state:
    st.session_state.cheat_violations = 0
if 'termination_reason' not in st.session_state:
    st.session_state.termination_reason = ""

# Read query parameters from URL
query_params = st.query_params
if 'candidate' in query_params and not st.session_state.candidate_name:
    st.session_state.candidate_name = query_params['candidate']
if 'job' in query_params and not st.session_state.job_role:
    st.session_state.job_role = query_params['job']
if 'interview_id' in query_params:
    st.session_state.interview_id = query_params['interview_id']
if 'email' in query_params:
    st.session_state.candidate_email = query_params['email']
if 'interviewer_id' in query_params:
    st.session_state.interviewer_id = query_params['interviewer_id']
if 'job_description' in query_params:
    st.session_state.job_description = query_params['job_description']
if 'difficulty_level' in query_params:
    st.session_state.difficulty_level = query_params['difficulty_level']
if 'auto_start' in query_params:
    st.session_state.auto_start_interview = query_params['auto_start'] == 'true'

# ---- Session snapshot helpers (persist evaluations across page reload) ----
import json as _json
import urllib.parse as _urlparse
_SNAPSHOT_FILE = Path(__file__).parent.parent / '.interview_snapshot.json'

def save_session_snapshot():
    """Save key session state to a file so it survives a page reload."""
    try:
        data = {
            'evaluations': st.session_state.get('evaluations', []),
            'candidate_name': st.session_state.get('candidate_name', ''),
            'job_role': st.session_state.get('job_role', ''),
            'total_questions': st.session_state.get('total_questions', 8),
            'video_enabled': st.session_state.get('video_enabled', False),
            'cheating_warnings': st.session_state.get('cheating_warnings', []),
        }
        _SNAPSHOT_FILE.write_text(_json.dumps(data))
    except Exception:
        pass

def load_session_snapshot():
    """Restore session state from snapshot file."""
    try:
        if _SNAPSHOT_FILE.exists():
            data = _json.loads(_SNAPSHOT_FILE.read_text())
            if data.get('evaluations'):
                st.session_state.evaluations = data['evaluations']
            if data.get('candidate_name'):
                st.session_state.candidate_name = data['candidate_name']
            if data.get('job_role'):
                st.session_state.job_role = data['job_role']
            if data.get('total_questions'):
                st.session_state.total_questions = data['total_questions']
            st.session_state.video_enabled = data.get('video_enabled', False)
            st.session_state.cheating_warnings = data.get('cheating_warnings', [])
            st.session_state.interview_started = True
            _SNAPSHOT_FILE.unlink(missing_ok=True)  # clean up after restoring
    except Exception:
        pass

# Check for termination triggered via URL query param from JS
if 'terminated' in query_params and query_params.get('terminated') == '1':
    if not st.session_state.exam_terminated:
        st.session_state.exam_terminated = True
        raw_reason = query_params.get('termination_reason', 'Exam rules violation')
        st.session_state.termination_reason = _urlparse.unquote(raw_reason)
        # Restore evaluations from snapshot so results can be shown
        load_session_snapshot()
    # Always ensure we route to interview_page (not setup_page) so the
    # termination banner and results are rendered.
    st.session_state.interview_started = True


def inject_anticheat_js():
    """Inject fullscreen prompt overlay + anti-cheat JavaScript via components.html.
    Fullscreen MUST be triggered by a real user click (browser security policy).
    This renders an overlay the user clicks to enter fullscreen and start the exam.
    """
    components.html("""
    <style>
    #fs-overlay {
        position:fixed;top:0;left:0;width:100%;height:100%;
        background:rgba(15,23,42,0.96);color:#fff;
        display:flex;flex-direction:column;align-items:center;justify-content:center;
        z-index:2147483647;font-family:Arial,sans-serif;text-align:center;
        cursor:pointer;padding:2rem;
    }
    #fs-overlay h1{font-size:2.4rem;margin:0 0 1rem 0;}
    #fs-overlay p{font-size:1.2rem;opacity:.85;margin:0 0 2rem 0;}
    #fs-btn{
        background:linear-gradient(135deg,#3b82f6,#2563eb);color:#fff;
        border:none;border-radius:14px;padding:1.1rem 3rem;
        font-size:1.3rem;font-weight:700;cursor:pointer;
        box-shadow:0 8px 25px rgba(59,130,246,.5);
    }
    </style>
    <script>
    (function() {
        var p = window.parent;
        var pd = p.document;

        // ---- Fullscreen helper ----
        function requestFullscreen() {
            var el = pd.documentElement;
            try {
                if (el.requestFullscreen) el.requestFullscreen();
                else if (el.webkitRequestFullscreen) el.webkitRequestFullscreen();
                else if (el.mozRequestFullScreen) el.mozRequestFullScreen();
                else if (el.msRequestFullscreen) el.msRequestFullscreen();
            } catch(e) {}
        }

        // ---- If anti-cheat already installed, skip overlay ----
        if (p._antiCheatActive) return;

        // ---- Show fullscreen prompt overlay on the parent page ----
        var existing = pd.getElementById('fs-overlay-parent');
        if (!existing) {
            var overlay = pd.createElement('div');
            overlay.id = 'fs-overlay-parent';
            overlay.style.cssText = [
                'position:fixed','top:0','left:0','width:100vw','height:100vh',
                'background:rgba(15,23,42,0.97)','color:#fff',
                'display:flex','flex-direction:column','align-items:center','justify-content:center',
                'z-index:2147483647','font-family:Arial,sans-serif','text-align:center',
                'cursor:pointer','padding:2rem'
            ].join(';');
            overlay.innerHTML =
                '<div style="font-size:5rem;margin-bottom:1.5rem;">&#128421;</div>' +
                '<h1 style="font-size:2.4rem;margin:0 0 0.8rem 0;">Secure Interview Mode</h1>' +
                '<p style="font-size:1.2rem;opacity:0.85;margin:0 0 2rem 0;max-width:520px;">'+
                'Click the button below to enter fullscreen and begin your exam.<br>'+
                'Leaving fullscreen, switching tabs, or using keyboard shortcuts will be flagged.</p>' +
                '<button id="fs-start-btn" style="background:linear-gradient(135deg,#3b82f6,#2563eb);color:#fff;'+
                'border:none;border-radius:14px;padding:1.1rem 3rem;font-size:1.3rem;font-weight:700;cursor:pointer;">'+
                '&#9654; Start Exam in Fullscreen</button>';

            overlay.querySelector('#fs-start-btn').addEventListener('click', function(e) {
                e.stopPropagation();
                // Fullscreen triggered by real user click
                requestFullscreen();
                overlay.style.display = 'none';
                installAntiCheat();
            });

            pd.body.appendChild(overlay);
        }

        function installAntiCheat() {
        p._antiCheatActive = true;
        p._violations = 0;
        p._violationLog = [];

        // Grace period: ignore all events for 4 seconds while Streamlit settles
        var acReady = false;
        setTimeout(function() { acReady = true; }, 4000);

        // Only flag fullscreen EXIT if we confirmed entry first
        var wasFullscreen = false;
        var noFaceCount = 0;  // consecutive no-face detection counter

        // ---- Terminate exam and redirect ----
        function terminateExam(reason) {
            if (p._examTerminated) return;
            p._examTerminated = true;

            // Full-screen red overlay
            var termDiv = pd.createElement('div');
            termDiv.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;'
                + 'background:rgba(127,0,0,0.97);color:#fff;display:flex;flex-direction:column;'
                + 'align-items:center;justify-content:center;z-index:2147483647;'
                + 'font-family:Arial,sans-serif;text-align:center;padding:2rem;';
            termDiv.innerHTML =
                '<div style="font-size:5rem;margin-bottom:1.2rem;">🚫</div>'
                + '<h1 style="font-size:2.8rem;margin:0 0 1rem 0;">Exam Terminated</h1>'
                + '<p style="font-size:1.3rem;opacity:0.9;max-width:600px;margin:0 0 2rem 0;">' + reason + '</p>'
                + '<p style="font-size:1.1rem;opacity:0.7;">Redirecting to results in 3 seconds…</p>';
            pd.body.appendChild(termDiv);

            setTimeout(function() {
                var url = new URL(p.location.href);
                url.searchParams.set('terminated', '1');
                url.searchParams.set('termination_reason', reason);
                p.location.href = url.toString();
            }, 3000);
        }

        // ---- Overlay helper (created on parent document) ----
        function showWarningOverlay(message, count) {
            // brief warning toast — does not block exam
        }

        // ---- Record violation ----
        function recordViolation(reason) {
            if (!acReady) return;  // ignore during grace period
            p._violations++;
            p._violationLog.push({ reason: reason, time: new Date().toISOString() });
        }

        // ---- Tab / visibility switch ----
        pd.addEventListener('visibilitychange', function() {
            if (pd.hidden) {
                recordViolation('Tab switch detected');
                terminateExam('You switched tabs or minimized the window. The exam has been automatically terminated.');
            }
        });

        // ---- Keyboard shortcuts — intercept in parent + ALL Streamlit iframes ----
        function keyHandler(e) {
            // Allow Ctrl/Meta combos — needed for copy, paste, select-all in text fields
            // (Ctrl+C, Ctrl+V, Ctrl+A, etc. are permitted during interview)

            // Alt+Tab — silently block (switching apps)
            if (e.altKey && (e.key === 'Tab' || e.key === 'tab')) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
            // Block only dangerous function keys: F12 (devtools), F11 (fullscreen toggle)
            // Allow F5 (refresh handled by browser) and others that aid accessibility
            if (e.keyCode === 123 || e.keyCode === 122) { // F12 or F11
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
            if (e.key === 'Escape') {
                e.preventDefault();
                setTimeout(requestFullscreen, 300);
            }
        }

        // Attach keyHandler to a document safely
        function attachKeyHandler(doc) {
            if (!doc) return;
            try { doc.addEventListener('keydown', keyHandler, true); } catch(e) {}
        }

        // Attach to parent document and all existing iframes
        attachKeyHandler(pd);
        var existingFrames = pd.querySelectorAll('iframe');
        for (var fi = 0; fi < existingFrames.length; fi++) {
            attachKeyHandler(existingFrames[fi].contentDocument);
        }

        // Watch for new iframes added by Streamlit and attach handler to them too
        var frameObserver = new MutationObserver(function(mutations) {
            mutations.forEach(function(m) {
                m.addedNodes.forEach(function(node) {
                    if (node.tagName === 'IFRAME') {
                        node.addEventListener('load', function() {
                            attachKeyHandler(node.contentDocument);
                        });
                        // Try immediately in case already loaded
                        attachKeyHandler(node.contentDocument);
                    }
                    // Also scan nested additions
                    if (node.querySelectorAll) {
                        var nested = node.querySelectorAll('iframe');
                        for (var ni = 0; ni < nested.length; ni++) {
                            attachKeyHandler(nested[ni].contentDocument);
                        }
                    }
                });
            });
        });
        frameObserver.observe(pd.body, { childList: true, subtree: true });

        // ---- Right-click ---- (block silently, don't count as violation)
        pd.addEventListener('contextmenu', function(e) {
            e.preventDefault();
            return false;
        }, true);

        // ---- Window blur ----
        // NOTE: In Streamlit every widget click triggers parent blur (iframe arch).
        // Instead we rely on visibilitychange for real tab-switches only.
        // (window blur listener intentionally removed to prevent false positives)

        // ---- Fullscreen exit detection ----
        function onFullscreenChange() {
            var isFS = !!(pd.fullscreenElement ||
                          pd.webkitFullscreenElement ||
                          pd.mozFullScreenElement ||
                          pd.msFullscreenElement);
            if (isFS) {
                wasFullscreen = true;  // confirmed entry
            } else if (wasFullscreen && acReady) {
                wasFullscreen = false;
                recordViolation('Fullscreen mode exited');
                terminateExam('You exited fullscreen mode. The exam has been automatically terminated.');
            }
        }
        pd.addEventListener('fullscreenchange', onFullscreenChange);
        pd.addEventListener('webkitfullscreenchange', onFullscreenChange);
        pd.addEventListener('mozfullscreenchange', onFullscreenChange);
        pd.addEventListener('MSFullscreenChange', onFullscreenChange);

        // ---- Disable text selection + drag on parent ----
        pd.addEventListener('selectstart', function(e) { e.preventDefault(); });
        pd.addEventListener('dragstart',   function(e) { e.preventDefault(); });

        // ---- Camera-based face monitoring (runs in parent context) ----
        function initCamera() {
            if (p._cameraActive) return;
            p._cameraActive = true;

            // Create camera panel injected into parent body
            var panel = pd.createElement('div');
            panel.id = 'cam-monitor-panel';
            panel.style.cssText = 'position:fixed;bottom:80px;left:20px;z-index:99998;'
                + 'width:200px;background:#1e3a5f;border-radius:10px;padding:8px;'
                + 'box-shadow:0 4px 16px rgba(0,0,0,0.5);font-family:Arial,sans-serif;';

            var vidEl = pd.createElement('video');
            vidEl.autoplay = true; vidEl.muted = true; vidEl.playsInline = true;
            vidEl.style.cssText = 'width:100%;border-radius:6px;display:block;background:#000;';
            panel.appendChild(vidEl);

            var canEl = pd.createElement('canvas');
            canEl.style.cssText = 'position:absolute;top:8px;left:8px;pointer-events:none;border-radius:6px;';
            panel.appendChild(canEl);

            var statEl = pd.createElement('div');
            statEl.style.cssText = 'color:#fff;font-size:11px;text-align:center;padding:4px 2px;margin-top:4px;border-radius:4px;';
            statEl.textContent = '⏳ Starting camera...';
            panel.appendChild(statEl);

            pd.body.appendChild(panel);

            // Request camera via parent navigator (bypasses iframe camera restriction)
            var nav = p.navigator || navigator;
            if (!nav.mediaDevices || !nav.mediaDevices.getUserMedia) {
                statEl.textContent = '❌ Camera API not available';
                return;
            }

            nav.mediaDevices.getUserMedia({video:{width:200,height:150}, audio:false})
            .then(function(stream) {
                vidEl.srcObject = stream;
                statEl.textContent = '⏳ Loading face model...';

                function loadScriptInParent(src, attr, cb) {
                    if (pd.querySelector('script['+attr+']')) { cb(); return; }
                    var s = pd.createElement('script');
                    s.setAttribute(attr, '1');
                    s.src = src;
                    s.onload = cb;
                    s.onerror = function() { statEl.textContent = '❌ Failed to load model'; };
                    pd.head.appendChild(s);
                }

                function startDetection() {
                    p.blazeface.load().then(function(model) {
                        statEl.textContent = '✅ Camera active';
                        var ctx = canEl.getContext('2d');
                        var camCooldown = false;

                        function detect() {
                            if (vidEl.readyState < 2) { setTimeout(detect, 500); return; }
                            canEl.width  = vidEl.videoWidth  || 200;
                            canEl.height = vidEl.videoHeight || 150;
                            ctx.clearRect(0, 0, canEl.width, canEl.height);

                            model.estimateFaces(vidEl, false).then(function(preds) {
                                preds.forEach(function(face) {
                                    var tl = face.topLeft, br = face.bottomRight;
                                    ctx.strokeStyle = '#22c55e'; ctx.lineWidth = 2;
                                    ctx.strokeRect(tl[0], tl[1], br[0]-tl[0], br[1]-tl[1]);
                                });

                                if (preds.length === 0) {
                                    statEl.style.background = '#dc2626';
                                    statEl.textContent = '🚨 No face detected!';
                                    if (acReady) {
                                        noFaceCount++;
                                        recordViolation('No face detected! Please stay visible to the camera.');
                                        if (noFaceCount >= 3) {
                                            terminateExam('No face was detected for an extended period. The exam has been automatically terminated due to missing candidate presence.');
                                        }
                                    }
                                } else if (preds.length > 1) {
                                    statEl.style.background = '#dc2626';
                                    statEl.textContent = '🚨 Multiple faces!';
                                    if (acReady) {
                                        noFaceCount = 0;
                                        recordViolation('Multiple persons detected in camera! This is strictly not allowed.');
                                        terminateExam('Multiple faces were detected in the camera. The exam has been automatically terminated.');
                                    }
                                } else {
                                    statEl.style.background = '#065f46';
                                    statEl.textContent = '✅ Face detected';
                                    noFaceCount = 0;  // reset on successful detection
                                }
                                setTimeout(detect, 2000);
                            }).catch(function(){ setTimeout(detect, 2000); });
                        }
                        detect();
                    }).catch(function(e){ statEl.textContent = '❌ Model error: '+e; });
                }

                loadScriptInParent(
                    'https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@4.17.0/dist/tf.min.js',
                    'data-proctoring-tf',
                    function() {
                        loadScriptInParent(
                            'https://cdn.jsdelivr.net/npm/@tensorflow-models/blazeface@0.0.7/dist/blazeface.min.js',
                            'data-proctoring-bf',
                            startDetection
                        );
                    }
                );
            }).catch(function(err) {
                statEl.textContent = '❌ Camera: ' + err.message;
                panel.style.background = '#7f1d1d';
            });
        }

        initCamera();

        console.log('[AntiCheat] Protection active on parent window.');
        } // end installAntiCheat
    })();
    </script>
    """, height=0)


# Custom CSS
st.markdown("""
<style>
    /* Global Styles */
    .stApp {
        background-color: #f8fafc;
        background-image: radial-gradient(#cbd5e1 1px, transparent 1px);
        background-size: 20px 20px;
    }
    
    .main {
        background: transparent;
        padding: 0;
        transition: opacity 0.2s ease-in-out;
    }
    
    .block-container {
        padding: 1rem 2rem;
        max-width: 100%;
        background: transparent;
        transition: all 0.2s ease-in-out;
    }
    
    [data-testid="stAppViewContainer"] {
        background: transparent;
    }
    
    /* Smooth transitions for all elements */
    * {
        transition: opacity 0.15s ease-in-out;
    }
    
    /* Header */
    .app-header {
        background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
        color: white;
        padding: 1.5rem 2rem;
        border-radius: 15px;
        margin-bottom: 1.5rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
        box-shadow: 0 10px 25px rgba(59, 130, 246, 0.2);
        position: relative;
        overflow: hidden;
    }
    
    .app-header::before {
        content: '';
        position: absolute;
        top: -50%;
        left: -50%;
        width: 200%;
        height: 200%;
        background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 60%);
        transform: rotate(30deg);
        pointer-events: none;
    }
    
    .app-title {
        font-size: 2.2rem;
        font-weight: 800;
        display: flex;
        align-items: center;
        gap: 1rem;
        text-shadow: 0 2px 4px rgba(0,0,0,0.2);
        z-index: 1;
    }
    
    .status-badge {
        background: rgba(255, 255, 255, 0.2);
        backdrop-filter: blur(10px);
        padding: 0.5rem 1.2rem;
        border-radius: 20px;
        font-size: 0.95rem;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        border: 1px solid rgba(255,255,255,0.3);
        z-index: 1;
    }
    
    .status-dot {
        width: 12px;
        height: 12px;
        background: #ef4444;
        border-radius: 50%;
        animation: pulse 2s infinite;
        box-shadow: 0 0 10px #ef4444;
    }
    
    @keyframes pulse {
        0%, 100% { opacity: 1; transform: scale(1); }
        50% { opacity: 0.5; transform: scale(1.2); }
    }
    
    /* Sidebar Panel */
    .sidebar-panel {
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(10px);
        border-radius: 20px;
        padding: 1.5rem;
        box-shadow: 0 10px 30px rgba(0,0,0,0.05);
        border: 1px solid rgba(255,255,255,0.5);
        height: 100%;
    }
    
    .panel-section {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 15px;
        padding: 1.2rem;
        margin-bottom: 1.2rem;
        box-shadow: 0 4px 6px rgba(0,0,0,0.02);
    }
    
    .panel-header {
        font-size: 1.1rem;
        font-weight: 800;
        color: #1e293b;
        margin-bottom: 1rem;
        text-transform: uppercase;
        letter-spacing: 1px;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .question-badge {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
        color: white;
        padding: 1rem;
        border-radius: 12px;
        text-align: center;
        font-size: 1.2rem;
        font-weight: bold;
        margin-bottom: 1rem;
        box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
    }
    
    .info-item {
        display: flex;
        align-items: center;
        gap: 0.8rem;
        padding: 0.6rem;
        color: #334155;
        font-weight: 600;
        font-size: 1.05rem;
        background: #f8fafc;
        border-radius: 8px;
        margin-bottom: 0.5rem;
    }
    
    .time-remaining {
        font-size: 1.8rem;
        font-weight: 800;
        color: #1e40af;
        text-align: center;
        padding: 1.5rem 1rem;
        background: #f0f9ff;
        border-radius: 12px;
        border: 2px dashed #bae6fd;
        margin-top: 1rem;
    }
    
    .criteria-item {
        display: flex;
        align-items: center;
        gap: 0.8rem;
        padding: 0.6rem;
        color: #334155;
        font-weight: 600;
        font-size: 1.05rem;
        background: #f8fafc;
        border-radius: 8px;
        margin-bottom: 0.5rem;
    }
    
    .check-icon {
        color: #10b981;
        font-size: 1.2rem;
        background: #d1fae5;
        border-radius: 50%;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
    
    /* Main Interview Area */
    .interview-container {
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(10px);
        border-radius: 20px;
        padding: 2.5rem;
        box-shadow: 0 10px 30px rgba(0,0,0,0.05);
        border: 1px solid rgba(255,255,255,0.5);
    }
    
    .ai-section {
        margin-bottom: 2.5rem;
    }
    
    .ai-header {
        color: #1e293b;
        font-size: 1.6rem;
        font-weight: 800;
        margin-bottom: 1.5rem;
        border-bottom: 3px solid #e2e8f0;
        padding-bottom: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 1px;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .ai-content {
        display: flex;
        gap: 2.5rem;
        align-items: flex-start;
    }
    
    .ai-avatar {
        width: 180px;
        height: 180px;
        border-radius: 50%;
        background-image: url('https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80');
        background-size: cover;
        background-position: center;
        border: 6px solid #eff6ff;
        box-shadow: 0 10px 25px rgba(59, 130, 246, 0.2), 0 0 0 2px #3b82f6;
        flex-shrink: 0;
        position: relative;
    }
    
    .ai-avatar::after {
        content: '';
        position: absolute;
        bottom: 10px;
        right: 10px;
        width: 20px;
        height: 20px;
        background: #10b981;
        border-radius: 50%;
        border: 3px solid white;
        box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
    }
    
    .question-bubble {
        background: #ffffff;
        border: 2px solid #e2e8f0;
        border-radius: 20px;
        padding: 2rem;
        font-size: 1.35rem;
        line-height: 1.8;
        color: #1e293b;
        font-weight: 600;
        box-shadow: 0 10px 25px rgba(0,0,0,0.05);
        flex-grow: 1;
        position: relative;
    }
    
    .question-bubble:before {
        content: '';
        position: absolute;
        left: -12px;
        top: 40px;
        width: 20px;
        height: 20px;
        background: #ffffff;
        border-left: 2px solid #e2e8f0;
        border-bottom: 2px solid #e2e8f0;
        transform: rotate(45deg);
    }
    
    /* Recording Section */
    .recording-section {
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        border-radius: 20px;
        padding: 2rem;
        margin-top: 2rem;
        color: white;
        box-shadow: 0 10px 25px rgba(59, 130, 246, 0.3);
        position: relative;
        overflow: hidden;
    }
    
    .recording-section::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: url('data:image/svg+xml;utf8,<svg width="100" height="100" xmlns="http://www.w3.org/2000/svg"><path d="M10 10h80v80h-80z" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="1"/></svg>');
        opacity: 0.5;
    }
    
    .recording-content {
        display: flex;
        align-items: center;
        gap: 2rem;
        position: relative;
        z-index: 1;
    }
    
    .mic-icon {
        font-size: 3.5rem;
        width: 90px;
        height: 90px;
        background: rgba(255,255,255,0.2);
        backdrop-filter: blur(5px);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        border: 2px solid rgba(255,255,255,0.4);
        box-shadow: 0 0 20px rgba(255,255,255,0.2);
        animation: pulse-mic 2s infinite;
    }
    
    @keyframes pulse-mic {
        0% { box-shadow: 0 0 0 0 rgba(255,255,255,0.4); }
        70% { box-shadow: 0 0 0 20px rgba(255,255,255,0); }
        100% { box-shadow: 0 0 0 0 rgba(255,255,255,0); }
    }
    
    .recording-info {
        flex-grow: 1;
    }
    
    .recording-title {
        font-size: 1.5rem;
        font-weight: 800;
        margin-bottom: 0.5rem;
        text-shadow: 0 2px 4px rgba(0,0,0,0.2);
    }
    
    .waveform {
        height: 50px;
        display: flex;
        align-items: center;
        gap: 4px;
    }
    
    .waveform-bar {
        width: 5px;
        background: rgba(255,255,255,0.9);
        border-radius: 3px;
        animation: wave 1s ease-in-out infinite;
    }
    
    @keyframes wave {
        0%, 100% { height: 15px; }
        50% { height: 50px; }
    }
    
    /* Action Buttons */
    .action-buttons {
        display: flex;
        gap: 1rem;
        margin-top: 2.5rem;
        justify-content: center;
    }
    
    .btn {
        padding: 1rem 2rem;
        border-radius: 12px;
        border: none;
        font-size: 1.1rem;
        font-weight: bold;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    
    .btn-pause {
        background: white;
        color: #1e40af;
        border: 2px solid #1e40af;
    }
    
    .btn-note {
        background: white;
        color: #1e40af;
        border: 2px solid #1e40af;
    }
    
    .btn-end {
        background: #ef4444;
        color: white;
        box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
    }
    
    .btn:hover {
        transform: translateY(-3px);
        box-shadow: 0 8px 20px rgba(0,0,0,0.15);
    }
    
    /* Setup Form */
    .setup-container {
        max-width: 700px;
        margin: 2rem auto;
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(10px);
        border-radius: 20px;
        padding: 3rem;
        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
        border: 1px solid rgba(255,255,255,0.5);
    }
    
    .setup-header {
        text-align: center;
        font-size: 2.5rem;
        font-weight: 900;
        color: #1e3a8a;
        margin-bottom: 1rem;
        text-shadow: 0 2px 4px rgba(0,0,0,0.05);
    }
    
    .setup-subtitle {
        text-align: center;
        font-size: 1.2rem;
        color: #64748b;
        margin-bottom: 2.5rem;
    }
    
    .hero-image {
        width: 100%;
        height: 250px;
        object-fit: cover;
        border-radius: 15px;
        margin-bottom: 2rem;
        box-shadow: 0 10px 25px rgba(0,0,0,0.1);
    }
    
    /* Hide Streamlit elements */
    #MainMenu {visibility: hidden;}
    footer {visibility: hidden;}
    header {visibility: hidden;}
    
    /* Video Monitoring */
    .video-container {
        background: #0f172a;
        border-radius: 15px;
        padding: 0.5rem;
        margin-bottom: 1rem;
        box-shadow: inset 0 0 20px rgba(0,0,0,0.5);
    }
    
    .video-frame {
        width: 100%;
        border-radius: 10px;
    }
    
    .warning-box {
        background: #fef2f2;
        border-left: 5px solid #ef4444;
        padding: 1.2rem;
        border-radius: 10px;
        margin: 0.8rem 0;
        animation: shake 0.5s;
        box-shadow: 0 4px 6px rgba(239, 68, 68, 0.1);
    }
    
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-10px); }
        75% { transform: translateX(10px); }
    }
    
    .warning-title {
        color: #b91c1c;
        font-weight: 800;
        font-size: 1.15rem;
        margin-bottom: 0.5rem;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .warning-text {
        color: #7f1d1d;
        font-weight: 500;
    }
    
    .proctoring-status {
        background: #f0fdf4;
        border: 2px solid #86efac;
        padding: 1rem;
        border-radius: 12px;
        text-align: center;
        color: #166534;
        font-weight: 800;
        font-size: 1.1rem;
        box-shadow: 0 4px 6px rgba(22, 101, 52, 0.05);
    }
    
    .proctoring-status.warning {
        background: #fefce8;
        border-color: #fde047;
        color: #854d0e;
    }
    
    .proctoring-status.alert {
        background: #fef2f2;
        border-color: #fca5a5;
        color: #991b1b;
    }
    
    .stButton button {
        width: 100%;
        border-radius: 12px;
        padding: 0.8rem;
        font-weight: 800;
        font-size: 1.1rem;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        border: 2px solid transparent;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }
    
    .stButton button[kind="primary"] {
        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
        color: white;
        box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
    }
    
    .stButton button:hover {
        transform: translateY(-3px);
        box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    }
    
    /* Improve text input visibility */
    .stTextInput input, .stTextArea textarea {
        font-size: 1.15rem !important;
        font-weight: 500 !important;
        color: #1e293b !important;
        background-color: #f8fafc !important;
        border: 2px solid #cbd5e1 !important;
        border-radius: 12px !important;
        line-height: 1.6 !important;
        padding: 14px !important;
        transition: all 0.2s;
    }
    
    .stTextInput input:focus, .stTextArea textarea:focus {
        border-color: #3b82f6 !important;
        box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15) !important;
        background-color: #ffffff !important;
    }
    
    /* Improve label visibility */
    label {
        font-weight: 800 !important;
        font-size: 1.1rem !important;
        color: #1e293b !important;
        margin-bottom: 8px !important;
    }
    
    /* Placeholder text styling */
    .stTextArea textarea::placeholder, .stTextInput input::placeholder {
        color: #94a3b8 !important;
        font-style: italic !important;
    }
</style>
""", unsafe_allow_html=True)


def format_time(seconds):
    """Format seconds to MM:SS"""
    mins = int(seconds // 60)
    secs = int(seconds % 60)
    return f"{mins:02d}:{secs:02d}"


def get_remaining_time():
    """Calculate remaining interview time"""
    if st.session_state.start_time is None:
        return st.session_state.interview_duration
    
    elapsed = (datetime.now() - st.session_state.start_time).total_seconds()
    remaining = max(0, st.session_state.interview_duration - elapsed)
    return remaining


def capture_video_frame():
    """Capture a frame from webcam with enhanced preprocessing for difficult lighting"""
    try:
        cap = cv2.VideoCapture(0)
        # Set camera properties for better detection
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        cap.set(cv2.CAP_PROP_AUTOFOCUS, 1)
        cap.set(cv2.CAP_PROP_AUTO_EXPOSURE, 1)
        
        ret, frame = cap.read()
        cap.release()
        
        if ret:
            # Enhanced preprocessing for backlighting scenarios
            # Convert to LAB color space
            lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            
            # Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
            # This significantly improves detection in backlit conditions
            clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
            l = clahe.apply(l)
            
            # Merge channels and convert back
            lab = cv2.merge([l, a, b])
            frame = cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)
            
            return frame
        return None
    except Exception as e:
        return None


def detect_cheating(frame):
    """Detect potential cheating behaviors using face detection"""
    warnings = []
    
    if frame is None:
        return warnings
    
    try:
        # Load face detection cascade
        face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        eye_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_eye.xml')
        
        # Convert to grayscale
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        # Apply CLAHE for better local contrast (especially for backlighting)
        clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
        gray = clahe.apply(gray)
        
        # Also apply regular histogram equalization
        gray = cv2.equalizeHist(gray)
        
        # Try multiple detection passes with different parameters
        # Balanced detection: sensitive but reduces false positives
        faces = face_cascade.detectMultiScale(
            gray, 
            scaleFactor=1.08,  # Balanced sensitivity
            minNeighbors=5,    # Higher threshold to reduce false positives
            minSize=(60, 60),  # Larger minimum size to filter noise
            flags=cv2.CASCADE_SCALE_IMAGE
        )
        
        # If no faces found, try alternative cascade with relaxed parameters
        if len(faces) == 0:
            alt_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_alt2.xml')
            faces = alt_cascade.detectMultiScale(
                gray,
                scaleFactor=1.1,
                minNeighbors=4,
                minSize=(50, 50)
            )
        
        # Filter out false positives based on size, aspect ratio, and position
        if len(faces) > 0:
            filtered_faces = []
            frame_height, frame_width = frame.shape[:2]
            
            for (x, y, w, h) in faces:
                # Calculate aspect ratio (should be close to 1.0 for real faces)
                aspect_ratio = w / h
                
                # Calculate face center
                face_center_x = x + w // 2
                face_center_y = y + h // 2
                
                # Filter criteria:
                # 1. Aspect ratio should be between 0.7 and 1.4 (roughly square)
                # 2. Face should be reasonably sized (at least 60x60)
                # 3. Face shouldn't be in extreme peripheral areas (outer 15% of frame)
                if (0.7 <= aspect_ratio <= 1.4 and 
                    w >= 60 and h >= 60 and
                    frame_width * 0.15 < face_center_x < frame_width * 0.85 and
                    frame_height * 0.1 < face_center_y < frame_height * 0.9):
                    filtered_faces.append((x, y, w, h))
            
            # If we filtered out all faces but had detections, keep the largest one
            # (likely the real face)
            if len(filtered_faces) == 0 and len(faces) > 0:
                # Keep the largest detection
                largest_face = max(faces, key=lambda f: f[2] * f[3])
                filtered_faces = [largest_face]
            
            faces = filtered_faces
        
        # Check number of faces
        if len(faces) == 0:
            warnings.append({
                'type': 'no_face',
                'message': '🚨 WARNING: No face detected! Please stay in front of the camera.',
                'severity': 'critical',
                'timestamp': datetime.now()
            })
        elif len(faces) > 1:
            warnings.append({
                'type': 'multiple_faces',
                'message': '🚨 CHEATING ALERT: Multiple people detected in the frame!',
                'severity': 'critical',
                'timestamp': datetime.now()
            })
        else:
            # Single face detected - check attention
            for (x, y, w, h) in faces:
                roi_gray = gray[y:y+h, x:x+w]
                roi_color = frame[y:y+h, x:x+w]
                eyes = eye_cascade.detectMultiScale(roi_gray)
                
                # Check if eyes are visible (person looking at screen)
                if len(eyes) < 1:
                    warnings.append({
                        'type': 'looking_away',
                        'message': '⚠️ WARNING: You appear to be looking away from the screen!',
                        'severity': 'high',
                        'timestamp': datetime.now()
                    })
                
                # Check face position (if too far to the side, might be looking at phone)
                frame_center_x = frame.shape[1] // 2
                face_center_x = x + w // 2
                
                if abs(face_center_x - frame_center_x) > frame.shape[1] * 0.3:
                    warnings.append({
                        'type': 'off_center',
                        'message': '⚠️ WARNING: Please center yourself in the frame and face the camera.',
                        'severity': 'medium',
                        'timestamp': datetime.now()
                    })
                
                # Check if face is too low (might be looking at phone on lap)
                frame_center_y = frame.shape[0] // 2
                face_center_y = y + h // 2
                
                if face_center_y > frame.shape[0] * 0.7:
                    warnings.append({
                        'type': 'looking_down',
                        'message': '🚨 WARNING: Looking down detected! Keep your eyes on the screen.',
                        'severity': 'high',
                        'timestamp': datetime.now()
                    })
        
        # Detect potential objects (phones) using edge detection
        edges = cv2.Canny(gray, 50, 150)
        contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        # Count significant rectangular objects (potential phones/papers)
        rectangular_objects = 0
        for contour in contours:
            area = cv2.contourArea(contour)
            if 500 < area < 50000:  # Filter by size
                peri = cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, 0.04 * peri, True)
                if len(approx) == 4:  # Quadrilateral shape
                    rectangular_objects += 1
        
        if rectangular_objects > 3:  # Multiple rectangular objects detected
            warnings.append({
                'type': 'objects_detected',
                'message': '⚠️ WARNING: Suspicious objects detected in frame! Remove any phones or papers.',
                'severity': 'high',
                'timestamp': datetime.now()
            })
        
        return warnings
        
    except Exception as e:
        return []


def draw_face_boxes(frame):
    """Draw boxes around detected faces and add warning overlays"""
    if frame is None:
        return frame
    
    try:
        face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        # Apply CLAHE for better local contrast
        clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
        gray = clahe.apply(gray)
        
        # Apply histogram equalization
        gray = cv2.equalizeHist(gray)
        
        # Detect faces with balanced parameters
        faces = face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.08,
            minNeighbors=5,
            minSize=(60, 60),
            flags=cv2.CASCADE_SCALE_IMAGE
        )
        
        # If no faces found, try alternative cascade
        if len(faces) == 0:
            alt_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_alt2.xml')
            faces = alt_cascade.detectMultiScale(
                gray,
                scaleFactor=1.1,
                minNeighbors=4,
                minSize=(50, 50)
            )
        
        # Filter out false positives based on size, aspect ratio, and position
        if len(faces) > 0:
            filtered_faces = []
            frame_height, frame_width = frame.shape[:2]
            
            for (x, y, w, h) in faces:
                # Calculate aspect ratio
                aspect_ratio = w / h
                
                # Calculate face center
                face_center_x = x + w // 2
                face_center_y = y + h // 2
                
                # Filter criteria for real faces
                if (0.7 <= aspect_ratio <= 1.4 and 
                    w >= 60 and h >= 60 and
                    frame_width * 0.15 < face_center_x < frame_width * 0.85 and
                    frame_height * 0.1 < face_center_y < frame_height * 0.9):
                    filtered_faces.append((x, y, w, h))
            
            # If all filtered out, keep the largest detection
            if len(filtered_faces) == 0 and len(faces) > 0:
                largest_face = max(faces, key=lambda f: f[2] * f[3])
                filtered_faces = [largest_face]
            
            faces = filtered_faces
        
        # Draw rectangles around faces
        for (x, y, w, h) in faces:
            color = (0, 255, 0) if len(faces) == 1 else (0, 0, 255)  # Green for 1 face, red for multiple
            cv2.rectangle(frame, (x, y), (x+w, y+h), color, 2)
        
        # Add warning overlays
        if len(faces) > 1:
            cv2.putText(frame, "CHEATING: Multiple Faces!", (10, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
        elif len(faces) == 0:
            cv2.putText(frame, "WARNING: No Face Detected", (10, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
        else:
            cv2.putText(frame, "Monitoring Active", (10, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
        
        # Add timestamp
        timestamp = datetime.now().strftime("%H:%M:%S")
        cv2.putText(frame, timestamp, (frame.shape[1] - 100, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)
        
        return frame
    except:
        return frame


def record_audio(duration=30):
    """Record audio from microphone - optimized for performance"""
    try:
        st.session_state.is_recording = True
        
        # Start recording (non-blocking)
        recording = sd.rec(
            int(duration * AUDIO_SAMPLE_RATE),
            samplerate=AUDIO_SAMPLE_RATE,
            channels=1,
            dtype='float32'
        )
        
        # Show lightweight progress (updates every 2 seconds instead of 1)
        progress_placeholder = st.empty()
        steps = duration // 2  # Update every 2 seconds for better performance
        for i in range(steps):
            time.sleep(2)
            elapsed = (i + 1) * 2
            if elapsed < duration:
                progress_placeholder.progress(elapsed / duration, f"🎤 Recording... {duration - elapsed}s remaining")
        
        # Wait for any remaining time and complete recording
        sd.wait()
        progress_placeholder.empty()
        
        st.session_state.is_recording = False
        
        return recording
        
    except Exception as e:
        st.error(f"Error recording audio: {e}")
        st.session_state.is_recording = False
        return None


def save_audio(audio_data, candidate_name):
    """Save audio recording to file - optimized for performance"""
    try:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{candidate_name}_{timestamp}.wav"
        filepath = AUDIO_STORAGE_PATH / filename
        
        # Create directory if it doesn't exist (only once)
        AUDIO_STORAGE_PATH.mkdir(parents=True, exist_ok=True)
        
        # Fast write operation
        sf.write(str(filepath), audio_data, AUDIO_SAMPLE_RATE)
        
        return str(filepath)
        
    except Exception as e:
        # Silent fail to avoid interrupting interview flow
        return None


def render_waveform(num_bars=30):
    """Render animated waveform"""
    heights = [10, 25, 40, 30, 20, 35, 40, 25, 15, 30] * 3
    bars_html = ""
    for i, height in enumerate(heights[:num_bars]):
        delay = i * 0.1
        bars_html += f'<div class="waveform-bar" style="height: {height}px; animation-delay: {delay}s;"></div>'
    
    return f'<div class="waveform">{bars_html}</div>'


def show_final_score():
    """Display final score summary"""
    st.markdown("""
    <div class="app-header">
        <div class="app-title">
            <span>🎉</span> Interview Complete!
        </div>
    </div>
    """, unsafe_allow_html=True)
    
    # Calculate scores
    if st.session_state.evaluations:
        total_scores = sum(e.get('total', 0) for e in st.session_state.evaluations)
        avg_score = total_scores / len(st.session_state.evaluations)
        
        technical_avg = sum(e.get('technical', 0) for e in st.session_state.evaluations) / len(st.session_state.evaluations)
        clarity_avg = sum(e.get('clarity', 0) for e in st.session_state.evaluations) / len(st.session_state.evaluations)
        communication_avg = sum(e.get('communication', 0) for e in st.session_state.evaluations) / len(st.session_state.evaluations)
    else:
        avg_score = 0
        technical_avg = clarity_avg = communication_avg = 0
    
    # Update interview status to COMPLETED and save scores in database
    if 'interview_id' in st.session_state and st.session_state.interview_id:
        try:
            from models.database import InterviewStatus
            db = SessionLocal()
            interview = db.query(Interview).filter(Interview.id == int(st.session_state.interview_id)).first()
            if interview:
                interview.status = InterviewStatus.COMPLETED
                # Populate identifying fields from session state
                if not interview.candidate_name and st.session_state.candidate_name:
                    interview.candidate_name = st.session_state.candidate_name
                if not interview.candidate_email and st.session_state.get('candidate_email'):
                    interview.candidate_email = st.session_state.candidate_email
                if not interview.job_title and st.session_state.job_role:
                    interview.job_title = st.session_state.job_role
                if not interview.interviewer_id and st.session_state.get('interviewer_id'):
                    try:
                        interview.interviewer_id = int(st.session_state.interviewer_id)
                    except (ValueError, TypeError):
                        pass
                # Save scores (scale to 100)
                interview.technical_score = int(technical_avg * 10)
                interview.communication_score = int(communication_avg * 10)
                interview.problem_solving_score = int(clarity_avg * 10)  # Map clarity to problem solving
                interview.total_score = int(avg_score * 10)
                interview.rating = int(avg_score)  # 1-10 rating
                
                # Generate summary feedback
                feedback_lines = []
                feedback_lines.append(f"Overall Performance: {avg_score:.1f}/10")
                feedback_lines.append(f"Technical Skills: {technical_avg:.1f}/10")
                feedback_lines.append(f"Communication: {communication_avg:.1f}/10")
                feedback_lines.append(f"Problem Solving: {clarity_avg:.1f}/10")
                
                if avg_score >= 7:
                    feedback_lines.append("\nRecommendation: Strong candidate, proceed to next round.")
                elif avg_score >= 5:
                    feedback_lines.append("\nRecommendation: Average candidate, requires further evaluation.")
                else:
                    feedback_lines.append("\nRecommendation: Below expectations, consider other candidates.")
                
                interview.feedback = "\n".join(feedback_lines)
                
                db.commit()
            db.close()
        except Exception as e:
            pass  # Silent fail to not interrupt user experience

    # Notify Java backend to update application status automatically
    if 'interview_id' in st.session_state and st.session_state.interview_id:
        try:
            import requests as _requests
            _passed = avg_score >= 7
            _recommendation = "HIRE" if avg_score >= 7 else "REJECT"
            _payload = {
                "status": "COMPLETED",
                "rating": int(avg_score),
                "passed": _passed,
                "recommendation": _recommendation,
                "feedback": (
                    f"Overall Performance: {avg_score:.1f}/10\n"
                    f"Technical Skills: {technical_avg:.1f}/10\n"
                    f"Communication: {communication_avg:.1f}/10\n"
                    f"Problem Solving: {clarity_avg:.1f}/10\n\n"
                    f"Recommendation: {'Strong candidate, proceed to hire.' if _passed else 'Below threshold, consider other candidates.'}"
                )
            }
            _requests.post(
                f"http://localhost:8089/api/interviews/{st.session_state.interview_id}/ai-result",
                json=_payload,
                timeout=5
            )
        except Exception:
            pass  # Silent fail
    
    # Main container
    st.markdown('<div style="max-width: 900px; margin: 2rem auto; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); border-radius: 20px; padding: 3rem; box-shadow: 0 20px 40px rgba(0,0,0,0.1); border: 1px solid rgba(255,255,255,0.5);">', unsafe_allow_html=True)
    
    # Candidate Info
    st.markdown(f'''
    <div style="text-align: center; margin-bottom: 2rem;">
        <img src="https://images.unsplash.com/photo-1552664730-d307ca884978?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" style="width: 100%; height: 200px; object-fit: cover; border-radius: 15px; margin-bottom: 2rem; box-shadow: 0 10px 25px rgba(0,0,0,0.1);">
        <h2 style="color: #1e3a8a; margin-bottom: 0.5rem; font-size: 2.5rem; font-weight: 900;">Interview Results</h2>
        <p style="font-size: 1.3rem; color: #475569;">Candidate: <strong style="color: #1e293b;">{st.session_state.candidate_name}</strong></p>
        <p style="font-size: 1.2rem; color: #475569;">Position: <strong style="color: #1e293b;">{st.session_state.job_role}</strong></p>
    </div>
    ''', unsafe_allow_html=True)
    
    # Overall Score
    score_color = "#10b981" if avg_score >= 7 else "#f59e0b" if avg_score >= 5 else "#ef4444"
    st.markdown(f'''
    <div style="text-align: center; margin: 2rem 0;">
        <div style="background: linear-gradient(135deg, {score_color} 0%, {score_color}dd 100%); color: white; border-radius: 20px; padding: 3rem; margin-bottom: 2rem; box-shadow: 0 15px 30px {score_color}40; position: relative; overflow: hidden;">
            <div style="position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 60%); transform: rotate(30deg); pointer-events: none;"></div>
            <h1 style="font-size: 5rem; margin: 0; color: white; font-weight: 900; text-shadow: 0 4px 10px rgba(0,0,0,0.2); position: relative; z-index: 1;">{avg_score:.1f}</h1>
            <p style="font-size: 1.5rem; margin: 0.5rem 0 0 0; color: rgba(255,255,255,0.9); font-weight: 600; position: relative; z-index: 1;">Overall Score / 10</p>
        </div>
    </div>
    ''', unsafe_allow_html=True)
    
    # Score breakdown
    col1, col2, col3, col4 = st.columns(4)
    
    with col1:
        st.metric("Questions Answered", f"{len(st.session_state.evaluations)}/{st.session_state.total_questions}")
    
    with col2:
        st.metric("Technical Score", f"{technical_avg:.1f}/10")
    
    with col3:
        st.metric("Clarity Score", f"{clarity_avg:.1f}/10")
    
    with col4:
        st.metric("Communication", f"{communication_avg:.1f}/10")
    
    st.markdown("<br>", unsafe_allow_html=True)
    
    # Proctoring Summary
    if st.session_state.video_enabled and st.session_state.cheating_warnings:
        st.markdown('<h3 style="color: #dc2626; margin-top: 2rem;">⚠️ Proctoring Alerts</h3>', unsafe_allow_html=True)
        
        total_warnings = len(st.session_state.cheating_warnings)
        critical_warnings = sum(1 for w in st.session_state.cheating_warnings if w.get('severity') == 'critical')
        
        col1, col2, col3 = st.columns(3)
        with col1:
            st.metric("Total Warnings", total_warnings, delta=None)
        with col2:
            st.metric("Critical Alerts", critical_warnings, delta=None)
        with col3:
            integrity_score = max(0, 100 - (total_warnings * 5) - (critical_warnings * 15))
            st.metric("Integrity Score", f"{integrity_score}%")
        
        # Show warning details
        with st.expander("View Warning Details"):
            for i, warning in enumerate(st.session_state.cheating_warnings, 1):
                timestamp = warning.get('timestamp', datetime.now()).strftime("%H:%M:%S")
                severity_icon = "🚨" if warning.get('severity') == 'critical' else "⚠️"
                st.markdown(f"**{i}. [{timestamp}]** {severity_icon} {warning.get('message', 'Warning detected')}")
    
    # Individual question scores
    st.markdown('<h3 style="color: #1e3a8a; margin-top: 2.5rem; font-weight: 800; border-bottom: 2px solid #e2e8f0; padding-bottom: 0.5rem;">Question-by-Question Breakdown</h3>', unsafe_allow_html=True)
    
    for i, evaluation in enumerate(st.session_state.evaluations, 1):
        score = evaluation.get('total', 0)
        feedback = evaluation.get('feedback', 'No feedback available')
        
        score_bg = "#f0fdf4" if score >= 7 else "#fefce8" if score >= 5 else "#fef2f2"
        score_color = "#10b981" if score >= 7 else "#f59e0b" if score >= 5 else "#ef4444"
        
        st.markdown(f'''
        <div style="background: {score_bg}; border-left: 5px solid {score_color}; border-radius: 12px; padding: 1.5rem; margin-bottom: 1.5rem; box-shadow: 0 4px 6px rgba(0,0,0,0.02);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                <strong style="color: #1e293b; font-size: 1.2rem;">Question {i}</strong>
                <span style="background: {score_color}; color: white; padding: 0.4rem 1rem; border-radius: 20px; font-weight: 800; font-size: 1.1rem; box-shadow: 0 2px 4px {score_color}40;">
                    {score:.1f}/10
                </span>
            </div>
            <p style="color: #475569; margin: 0; line-height: 1.6; font-size: 1.05rem;">{feedback}</p>
        </div>
        ''', unsafe_allow_html=True)
    
    st.markdown('</div>', unsafe_allow_html=True)
    
    # Action buttons
    col1, col2 = st.columns([1, 1])
    with col1:
        if st.button("🔄 Start New Interview", use_container_width=True, type="primary"):
            # Reset session state
            for key in list(st.session_state.keys()):
                del st.session_state[key]
            st.rerun()
    
    with col2:
        if st.button("📊 View Dashboard", use_container_width=True):
            st.info("Dashboard feature coming soon!")
    
    st.balloons()



def setup_page():
    """Interview setup page"""
    
    # Auto-start interview if flag is set and required params are available
    if st.session_state.auto_start_interview and st.session_state.candidate_name and st.session_state.job_role:
        # Only auto-start once
        st.session_state.auto_start_interview = False
        
        # Show processing message
        with st.spinner("🤖 Analyzing job requirements and starting interview..."):
            ai_service = get_ai_service()
            
            # Analyze job posting
            config = ai_service.analyze_job_posting(
                st.session_state.job_role, 
                st.session_state.job_description
            )
            
            # Extract configuration
            extracted_skills = config.get("skills", [])
            num_questions = config.get("num_questions", 8)
            duration_mins = config.get("duration_mins", 20)
            difficulty_distribution = config.get("difficulty_distribution", 
                                                 {"basic": 30, "intermediate": 50, "advanced": 20})
            experience_level = config.get("experience_level", "mid")
            
            # Store in session state
            st.session_state.extracted_skills = extracted_skills
            st.session_state.difficulty_distribution = difficulty_distribution
            st.session_state.experience_level = experience_level
            st.session_state.total_questions = num_questions
            st.session_state.interview_duration = duration_mins * 60
            st.session_state.interview_started = True
            st.session_state.start_time = datetime.now()
            st.session_state.question_count = 0
            st.session_state.video_enabled = True  # Enable video proctoring by default
            save_session_snapshot()  # persist initial state for termination recovery
            
            # Populate identifying fields in database when interview starts
            if 'interview_id' in st.session_state and st.session_state.interview_id:
                try:
                    db = SessionLocal()
                    interview = db.query(Interview).filter(Interview.id == int(st.session_state.interview_id)).first()
                    if interview:
                        # Populate identifying fields from session state
                        if not interview.candidate_name and st.session_state.candidate_name:
                            interview.candidate_name = st.session_state.candidate_name
                        if not interview.candidate_email and st.session_state.get('candidate_email'):
                            interview.candidate_email = st.session_state.candidate_email
                        if not interview.job_title and st.session_state.job_role:
                            interview.job_title = st.session_state.job_role
                        if not interview.interviewer_id and st.session_state.get('interviewer_id'):
                            try:
                                interview.interviewer_id = int(st.session_state.interviewer_id)
                            except (ValueError, TypeError):
                                pass
                        db.commit()
                    db.close()
                except:
                    pass
            
            # Determine question types
            text_question_ratio = 0.35
            num_text_questions = max(1, int(num_questions * text_question_ratio))
            import random
            all_question_indices = list(range(1, num_questions + 1))
            text_question_indices = random.sample(all_question_indices, num_text_questions)
            st.session_state.question_types = ['text' if i in text_question_indices else 'voice' 
                                               for i in range(1, num_questions + 1)]
            
            # Generate first question
            try:
                st.session_state.generating_question = True
                skills_str = ", ".join(extracted_skills) if extracted_skills else ""
                difficulty = ai_service.get_difficulty_for_question(1, num_questions, difficulty_distribution)
                
                question = ai_service.generate_question(
                    job_role=st.session_state.job_role,
                    skills=skills_str,
                    difficulty=difficulty,
                    previous_questions=[]
                )
                st.session_state.generating_question = False
                
                if question and len(question.strip()) > 0 and "error" not in question.lower():
                    st.session_state.current_question = question
                    st.session_state.previous_questions.append(question)
                    st.session_state.question_count = 1
                    st.session_state.answer_mode = st.session_state.question_types[0]
                    st.session_state.last_spoken_question = ""
                    st.session_state.question_ready = True
                else:
                    raise Exception("Invalid question received")
            except:
                st.session_state.generating_question = False
                st.session_state.current_question = f"Tell me about your experience with {st.session_state.job_role} and what makes you a good fit for this role?"
                st.session_state.previous_questions.append(st.session_state.current_question)
                st.session_state.question_count = 1
                st.session_state.answer_mode = st.session_state.question_types[0]
                st.session_state.last_spoken_question = ""
                st.session_state.question_ready = True
        
        st.success(f"""✅ Interview Auto-Started:
- **Skills**: {', '.join(extracted_skills)}
- **Duration**: {duration_mins} minutes
- **Questions**: {num_questions}
- **Difficulty**: {difficulty_distribution['basic']}% Basic | {difficulty_distribution['intermediate']}% Intermediate | {difficulty_distribution['advanced']}% Advanced""")
        
        # Rerun to show interview page
        import time
        time.sleep(1)
        st.rerun()
        return
    
    st.markdown("""
    <div class="app-header">
        <div class="app-title">
            <span>🤖</span> AI Interview System
        </div>
    </div>
    """, unsafe_allow_html=True)
    
    st.markdown('<div class="setup-container">', unsafe_allow_html=True)
    st.markdown('<img src="https://images.unsplash.com/photo-1516321318423-f06f85e504b3?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" class="hero-image">', unsafe_allow_html=True)
    st.markdown('<div class="setup-header">Welcome to AI Interview</div>', unsafe_allow_html=True)
    st.markdown('<div class="setup-subtitle">Configure your intelligent interview session</div>', unsafe_allow_html=True)
    
    # Tab selection for Smart vs Manual mode
    # Auto-select Smart Mode if job_description is provided via URL
    default_mode = 0 if st.session_state.job_description else 0
    setup_mode = st.radio(
        "Setup Mode",
        ["🤖 Smart Mode (AI Auto-Configuration)", "✍️ Manual Mode"],
        horizontal=True,
        index=default_mode,
        help="Smart Mode automatically extracts role, skills, and configures interview parameters from job posting"
    )
    
    with st.form("interview_setup"):
        candidate_name = st.text_input(
            "Your Name", 
            value=st.session_state.candidate_name,
            placeholder="Enter your full name"
        )
        
        if setup_mode.startswith("🤖"):
            # SMART MODE - AI Auto-Configuration
            st.markdown("### 📋 Job Posting")
            job_role = st.text_input(
                "Job Title/Role *", 
                value=st.session_state.job_role,
                placeholder="e.g., Software Engineer, Senior Data Scientist"
            )
            
            job_description = st.text_area(
                "Job Description (Optional - for better skill extraction)",
                value=st.session_state.job_description,
                height=150,
                placeholder="""Paste job description here for automatic skill extraction:
                
Example:
- Design and develop scalable web applications
- Work with React, Node.js, and PostgreSQL
- Implement RESTful APIs and microservices
- Collaborate with cross-functional teams"""
            )
            
            st.info("🔥 AI will automatically: Extract skills | Decide duration | Set question count | Assign difficulty levels")
            
            # These will be set automatically
            num_questions = None
            duration_mins = None
            
        else:
            # MANUAL MODE - Traditional form
            job_role = st.text_input(
                "Job Role", 
                value=st.session_state.job_role,
                placeholder="e.g., Software Engineer, Data Scientist"
            )
            
            job_description = ""  # Not used in manual mode
            
            col1, col2 = st.columns(2)
            with col1:
                num_questions = st.number_input("Number of Questions", 3, 15, 8)
            with col2:
                duration_mins = st.number_input("Duration (minutes)", 5, 60, 10)
        
        # Video proctoring option
        st.markdown("---")
        video_proctoring = st.checkbox("📹 Enable Video Proctoring (Recommended)", value=True,
                                       help="Monitors for cheating behavior via webcam — detects no face or multiple faces.")
        
        submit = st.form_submit_button("🚀 Start Interview", use_container_width=True)
        
        if submit:
            if candidate_name and job_role:
                # Smart Mode: Use AI to analyze job posting
                if setup_mode.startswith("🤖"):
                    with st.spinner("🤖 Analyzing job requirements and configuring interview..."):
                        ai_service = get_ai_service()
                        config = ai_service.analyze_job_posting(job_role, job_description)
                        
                        # Extract configuration
                        extracted_skills = config.get("skills", [])
                        num_questions = config.get("num_questions", 8)
                        duration_mins = config.get("duration_mins", 20)
                        difficulty_distribution = config.get("difficulty_distribution", 
                                                             {"basic": 30, "intermediate": 50, "advanced": 20})
                        experience_level = config.get("experience_level", "mid")
                        
                        # Store in session state
                        st.session_state.extracted_skills = extracted_skills
                        st.session_state.difficulty_distribution = difficulty_distribution
                        st.session_state.experience_level = experience_level
                        
                        # Show configuration summary
                        st.success(f"""✅ Interview Configured:
- **Skills Identified**: {', '.join(extracted_skills)}
- **Duration**: {duration_mins} minutes
- **Questions**: {num_questions}
- **Difficulty**: {difficulty_distribution['basic']}% Basic | {difficulty_distribution['intermediate']}% Intermediate | {difficulty_distribution['advanced']}% Advanced
- **Level**: {experience_level.title()}""")
                
                st.session_state.candidate_name = candidate_name
                st.session_state.job_role = job_role
                st.session_state.total_questions = num_questions
                st.session_state.interview_duration = duration_mins * 60
                st.session_state.interview_started = True
                st.session_state.start_time = datetime.now()
                st.session_state.question_count = 0
                st.session_state.video_enabled = video_proctoring
                save_session_snapshot()  # persist initial state so termination can restore candidate info
                
                # Populate identifying fields in database when interview starts
                if 'interview_id' in st.session_state and st.session_state.interview_id:
                    try:
                        db = SessionLocal()
                        interview = db.query(Interview).filter(Interview.id == int(st.session_state.interview_id)).first()
                        if interview:
                            # Populate identifying fields from session state
                            if not interview.candidate_name and st.session_state.candidate_name:
                                interview.candidate_name = st.session_state.candidate_name
                            if not interview.candidate_email and st.session_state.get('candidate_email'):
                                interview.candidate_email = st.session_state.candidate_email
                            if not interview.job_title and st.session_state.job_role:
                                interview.job_title = st.session_state.job_role
                            if not interview.interviewer_id and st.session_state.get('interviewer_id'):
                                try:
                                    interview.interviewer_id = int(st.session_state.interviewer_id)
                                except (ValueError, TypeError):
                                    pass
                            db.commit()
                        db.close()
                    except:
                        pass
                
                # Determine which questions should be text-based (coding/logic questions)
                text_question_ratio = 0.35
                num_text_questions = max(1, int(num_questions * text_question_ratio))
                
                import random
                all_question_indices = list(range(1, num_questions + 1))
                text_question_indices = random.sample(all_question_indices, num_text_questions)
                st.session_state.question_types = ['text' if i in text_question_indices else 'voice' 
                                                   for i in range(1, num_questions + 1)]
                
                # Generate first question immediately (optimized)
                with st.spinner("🤖 Generating your first question..."):
                    try:
                        st.session_state.generating_question = True
                        ai_service = get_ai_service()
                        
                        # Get skills and difficulty from session state (Smart Mode) or defaults
                        skills_list = st.session_state.get('extracted_skills', [])
                        skills_str = ", ".join(skills_list) if skills_list else ""
                        
                        difficulty_dist = st.session_state.get('difficulty_distribution', 
                                                               {"basic": 30, "intermediate": 50, "advanced": 20})
                        
                        # Get difficulty for question 1
                        difficulty = ai_service.get_difficulty_for_question(1, num_questions, difficulty_dist)
                        
                        question = ai_service.generate_question(
                            job_role=job_role,
                            skills=skills_str,
                            difficulty=difficulty,
                            previous_questions=[]
                        )
                        st.session_state.generating_question = False
                        
                        if question and len(question.strip()) > 0 and "error" not in question.lower():
                            st.session_state.current_question = question
                            st.session_state.previous_questions.append(question)
                            st.session_state.question_count = 1
                            st.session_state.answer_mode = st.session_state.question_types[0]
                            st.session_state.last_spoken_question = ""
                            st.session_state.question_ready = True
                        else:
                            raise Exception("Invalid question received")
                            
                    except Exception as e:
                        st.session_state.generating_question = False
                        # Fallback question
                        st.session_state.current_question = f"Tell me about your experience with {job_role} and what makes you a good fit for this role?"
                        st.session_state.previous_questions.append(st.session_state.current_question)
                        st.session_state.question_count = 1
                        st.session_state.answer_mode = st.session_state.question_types[0] if st.session_state.question_types else 'voice'
                        st.session_state.last_spoken_question = ""
                        st.session_state.question_ready = True
                
                st.rerun()
            else:
                st.error("Please fill in all fields")
    
    st.markdown('</div>', unsafe_allow_html=True)


def interview_page():
    """Main interview interface"""

    # --- Backup termination check (catches race conditions on redirect) ---
    _qp = st.query_params
    if 'terminated' in _qp and _qp.get('terminated') == '1':
        if not st.session_state.exam_terminated:
            st.session_state.exam_terminated = True
            _raw = _qp.get('termination_reason', 'Exam rules violation')
            st.session_state.termination_reason = _urlparse.unquote(_raw)
            load_session_snapshot()
        st.session_state.interview_started = True

    # --- Check for exam termination ---
    if st.session_state.exam_terminated:
        reason = st.session_state.get('termination_reason', 'Exam rules violation detected.')
        st.markdown(f"""
        <div style="background:linear-gradient(135deg,#7f1d1d,#dc2626);
             border-radius:16px;padding:2rem 2.5rem;margin-bottom:2rem;
             font-family:Arial,sans-serif;text-align:center;color:white;
             box-shadow:0 8px 32px rgba(220,38,38,0.4);">
            <div style="font-size:3.5rem;margin-bottom:0.75rem;">🚫</div>
            <h1 style="font-size:2.2rem;margin:0 0 0.75rem 0;font-weight:900;">Exam Terminated</h1>
            <p style="font-size:1.1rem;max-width:650px;margin:0 auto 1.25rem auto;
               opacity:0.93;line-height:1.7;">{reason}</p>
            <div style="background:rgba(255,255,255,0.15);border-radius:10px;
                 padding:0.75rem 1.5rem;display:inline-block;font-size:1rem;opacity:0.88;">
                Your answers have been saved. Results are shown below.
            </div>
        </div>
        """, unsafe_allow_html=True)
        # Remove any JS-injected overlay divs that may have survived
        components.html("""
        <script>
        (function(){
            var p = window.parent, pd = p.document;
            ['fs-overlay-parent','cam-monitor-panel'].forEach(function(id){
                var el = pd.getElementById(id);
                if (el) el.parentNode.removeChild(el);
            });
            // Remove any position:fixed z-index overlays added by terminateExam()
            pd.querySelectorAll('body > div[style*="2147483647"]').forEach(function(el){
                el.parentNode.removeChild(el);
            });
        })();
        </script>
        """, height=0)
        show_final_score()
        return

    # ---- Anti-cheat: inject fullscreen + monitoring JS ----
    inject_anticheat_js()

    # Check if interview is complete (all questions answered)
    if len(st.session_state.evaluations) >= st.session_state.total_questions:
        show_final_score()
        return
    
    # Header
    st.markdown(f"""
    <div class="app-header">
        <div class="app-title">
            <span>🤖</span> AI Interview System
        </div>
        <div class="status-badge">
            <span class="status-dot"></span>
            Interview in Progress...
        </div>
    </div>
    """, unsafe_allow_html=True)
    
    # Main layout
    col_sidebar, col_main = st.columns([1, 3])
    
    with col_sidebar:
        st.markdown('<div class="sidebar-panel">', unsafe_allow_html=True)
        
        # Interview Overview
        st.markdown('<div class="panel-section">', unsafe_allow_html=True)
        st.markdown('<div class="panel-header">Interview Overview</div>', unsafe_allow_html=True)
        
        # Determine current question mode
        current_mode = st.session_state.question_types[st.session_state.question_count - 1] if st.session_state.question_count <= len(st.session_state.question_types) else 'voice'
        mode_icon = "⌨️" if current_mode == 'text' else "🎤"
        mode_text = "Type Answer" if current_mode == 'text' else "Voice Answer"
        
        st.markdown(f'''
        <div class="question-badge">
            💬 Question {st.session_state.question_count} of {st.session_state.total_questions}
        </div>
        <div class="info-item">
            <span>🎯</span> Current Question
        </div>
        <div class="info-item">
            <span>{mode_icon}</span> <strong>{mode_text}</strong>
        </div>
        ''', unsafe_allow_html=True)
        
        # Replay question button
        if st.button("🔊 Replay Question", use_container_width=True):
            try:
                tts_service = get_tts_service()
                tts_service.speak(st.session_state.current_question)
                st.success("Playing question...")
            except Exception as e:
                st.error(f"Text-to-speech not available: {e}")
        
        remaining = get_remaining_time()
        
        # Add JavaScript timer that updates every second
        timer_html = f"""
        <div class="time-remaining" id="timer-display">
            ⏱️ Time Remaining: {format_time(remaining)}
        </div>
        <script>
        let startTime = {st.session_state.start_time.timestamp() if st.session_state.start_time else 'null'};
        let duration = {st.session_state.interview_duration};
        
        if (startTime) {{
            setInterval(function() {{
                let now = Date.now() / 1000;
                let elapsed = now - startTime;
                let remaining = Math.max(0, duration - elapsed);
                let mins = Math.floor(remaining / 60);
                let secs = Math.floor(remaining % 60);
                let timeStr = String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
                
                let timerEl = document.getElementById('timer-display');
                if (timerEl) {{
                    timerEl.innerHTML = '⏱️ Time Remaining: ' + timeStr;
                    
                    // Change color when time is low
                    if (remaining < 60) {{
                        timerEl.style.color = '#dc2626';
                        timerEl.style.fontWeight = 'bold';
                    }} else if (remaining < 180) {{
                        timerEl.style.color = '#ea580c';
                    }}
                }}
            }}, 1000);
        }}
        </script>
        """
        st.markdown(timer_html, unsafe_allow_html=True)
        
        st.markdown('</div>', unsafe_allow_html=True)
        
        # Evaluation Criteria
        st.markdown('<div class="panel-section">', unsafe_allow_html=True)
        st.markdown('<div class="panel-header">Evaluation Criteria</div>', unsafe_allow_html=True)
        
        criteria = ["Problem Solving", "Communication", "Adaptability"]
        for criterion in criteria:
            st.markdown(f'''
            <div class="criteria-item">
                <span class="check-icon">✓</span> {criterion}
            </div>
            ''', unsafe_allow_html=True)
        
        st.markdown('</div>', unsafe_allow_html=True)
        
        # Video Proctoring Section — camera panel is injected into parent DOM by anti-cheat JS
        if st.session_state.video_enabled and st.session_state.question_ready:
            st.markdown('<div class="panel-section">', unsafe_allow_html=True)
            st.markdown('<div class="panel-header">📹 Video Monitoring</div>', unsafe_allow_html=True)
            st.markdown("""
<div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:10px 14px;font-size:0.85rem;color:#1e40af;">
📷 <strong>Camera monitoring active</strong> — live face detection is running in the corner of your screen.<br>
<span style="font-size:0.8rem;color:#475569;">A small camera panel appears at the bottom-left of the page. Stay visible and centred.</span>
</div>""", unsafe_allow_html=True)
            if False:  # placeholder to keep old components.html block skipped
                components.html("""
<style>
  #cam-wrap { position:relative; width:100%; max-width:320px; margin:0 auto; }
  #cam-video { width:100%; border-radius:10px; border:2px solid #3b82f6; background:#000; }
  #cam-canvas { position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; border-radius:10px; }
  #cam-status { margin-top:6px; text-align:center; font-weight:bold; font-size:0.9rem; padding:4px 8px; border-radius:6px; }
  .ok  { background:#d1fae5; color:#065f46; }
  .warn{ background:#fef3c7; color:#92400e; }
  .err { background:#fee2e2; color:#991b1b; }
</style>
<div id="cam-wrap">
  <video id="cam-video" autoplay muted playsinline></video>
  <canvas id="cam-canvas"></canvas>
</div>
<div id="cam-status" class="ok">⏳ Starting camera...</div>

<script src="https://cdn.jsdelivr.net/npm/@tensorflow/tfjs@4.17.0/dist/tf.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@tensorflow-models/blazeface@0.0.7/dist/blazeface.min.js"></script>
<script>
(function() {
  var video   = document.getElementById('cam-video');
  var canvas  = document.getElementById('cam-canvas');
  var ctx     = canvas.getContext('2d');
  var status  = document.getElementById('cam-status');
  var model   = null;
  var cooldown = false;   // violation cooldown — one alert per 15 s

  function setStatus(msg, cls) {
    status.textContent = msg;
    status.className = cls;
  }

  function reportViolation(reason) {
    if (cooldown) return;
    cooldown = true;
    setTimeout(function() { cooldown = false; }, 15000);
    try { window.parent.postMessage({ type: 'face_violation', reason: reason }, '*'); } catch(e) {}
  }

  async function startCamera() {
    try {
      var stream = await navigator.mediaDevices.getUserMedia({ video: { width:320, height:240 }, audio:false });
      video.srcObject = stream;
      await video.play();
      setStatus('⏳ Loading face model...', 'ok');
      model = await blazeface.load();
      setStatus('✅ Camera active', 'ok');
      detectLoop();
    } catch(err) {
      setStatus('❌ Camera error: ' + err.message, 'err');
    }
  }

  async function detectLoop() {
    if (!model || video.readyState < 2) { requestAnimationFrame(detectLoop); return; }

    canvas.width  = video.videoWidth  || 320;
    canvas.height = video.videoHeight || 240;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    var predictions = [];
    try { predictions = await model.estimateFaces(video, false); } catch(e) {}

    // Draw bounding boxes
    predictions.forEach(function(p) {
      var s = p.topLeft, e = p.bottomRight;
      ctx.strokeStyle = '#22c55e';
      ctx.lineWidth   = 2;
      ctx.strokeRect(s[0], s[1], e[0]-s[0], e[1]-s[1]);
    });

    // Violation logic
    if (predictions.length === 0) {
      setStatus('🚨 No face detected!', 'err');
      reportViolation('No face detected! Please stay visible to the camera.');
    } else if (predictions.length > 1) {
      setStatus('🚨 Multiple faces detected!', 'err');
      reportViolation('Multiple persons detected in camera! This is strictly not allowed.');
    } else {
      setStatus('✅ Face detected — All clear', 'ok');
    }

    requestAnimationFrame(detectLoop);
  }

  startCamera();
})();
</script>
""", height=310)
            st.markdown('</div>', unsafe_allow_html=True)
        
        st.markdown('</div>', unsafe_allow_html=True)
    
    with col_main:
        st.markdown('<div class="interview-container">', unsafe_allow_html=True)
        
        # AI Interviewer Section
        st.markdown('<div class="ai-section">', unsafe_allow_html=True)
        st.markdown('<div class="ai-header"><span>🤖</span> AI Interviewer</div>', unsafe_allow_html=True)
        
        # Check if question exists
        if not st.session_state.current_question or st.session_state.current_question.strip() == "":
            st.error("⚠️ No question loaded. Please restart the interview.")
            if st.button("↩️ Restart Interview", use_container_width=True):
                st.session_state.clear()
                st.rerun()
        else:
            st.markdown(f'''
            <div class="ai-content">
                <div class="ai-avatar"></div>
                <div class="question-bubble">
                    {st.session_state.current_question}
                </div>
            </div>
            ''', unsafe_allow_html=True)
            
            # Speak question if not already spoken
            if st.session_state.current_question != st.session_state.last_spoken_question:
                try:
                    tts_service = get_tts_service()
                    tts_service.speak(st.session_state.current_question)
                    st.session_state.last_spoken_question = st.session_state.current_question
                except Exception as e:
                    pass  # TTS not critical
        
        st.markdown('</div>', unsafe_allow_html=True)
        
        # Answer Section - Different UI for Voice vs Text mode
        current_answer_mode = st.session_state.question_types[st.session_state.question_count - 1] if st.session_state.question_count <= len(st.session_state.question_types) else 'voice'
        
        if current_answer_mode == 'text':
            # Text/Code Input Mode
            st.markdown('''
            <div class="recording-section">
                <div class="recording-content">
                    <div class="mic-icon">
                        ⌨️
                    </div>
                    <div class="recording-info">
                        <div class="recording-title">
                            Type Your Answer or Code
                        </div>
                    </div>
                </div>
            </div>
            ''', unsafe_allow_html=True)
            
            # Code/Text editor
            st.markdown("### ✍️ Your Answer")
            st.info("💡 Type clearly - your text is highly visible with large, bold font")
            answer_text = st.text_area(
                "Type your answer here (you can write code, logic, or explanations)",
                height=300,
                placeholder="Start typing your answer here... You can write code, logic, or detailed explanations...",
                key=f"text_answer_{st.session_state.question_count}"
            )
            st.session_state.text_answer = answer_text
            
        else:
            # Voice Recording Mode - browser-native, instant start/stop
            st.markdown('''
            <div class="recording-section">
                <div class="recording-content">
                    <div class="mic-icon">🎤</div>
                    <div class="recording-info">
                        <div class="recording-title">Voice Answer</div>
                        <div style="font-size:0.9em;opacity:0.9;margin-top:4px;">
                            Press the mic button below to start recording.<br>
                            Press it again when done — no fixed time limit.
                        </div>
                    </div>
                </div>
            </div>
            ''', unsafe_allow_html=True)

            # Browser-native audio recorder (works instantly, no sounddevice)
            audio_value = st.audio_input(
                "🎙️ Click the microphone to record your answer",
                key=f"voice_rec_{st.session_state.question_count}"
            )

            if audio_value is not None:
                # Store bytes in session state so submit button can access them
                st.session_state.voice_bytes = audio_value.read()
                st.session_state.voice_answer_ready = True
                st.success("✅ Recording captured! Click **Submit Voice Answer** to evaluate.")
                st.markdown(render_waveform(), unsafe_allow_html=True)
            else:
                if not st.session_state.voice_answer_ready:
                    st.info("🎙️ Use the microphone above to record your answer, then click Submit.")
        
        st.markdown('</div>', unsafe_allow_html=True)
        
        # Action Buttons
        col1, col2, col3, col4, col5 = st.columns([1, 1, 1.2, 1, 1])
        
        with col1:
            if st.button("⏸️ Pause", use_container_width=True, disabled=st.session_state.is_recording):
                st.session_state.is_paused = not st.session_state.is_paused
                st.toast("⏸️ Interview paused" if st.session_state.is_paused else "▶️ Interview resumed")
        
        with col2:
            # Skip button
            if st.button("⏭️ Skip", use_container_width=True, 
                        disabled=st.session_state.is_recording):
                # Move to next question without evaluating
                st.warning("Question skipped")
                
                # Add empty evaluation for skipped question
                st.session_state.evaluations.append({
                    'total': 0,
                    'technical': 0,
                    'clarity': 0,
                    'communication': 0,
                    'feedback': 'Question was skipped'
                })
                save_session_snapshot()
                
                # Generate next question if not at limit
                if st.session_state.question_count < st.session_state.total_questions:
                    with st.spinner("🤖 Loading next question..."):
                        try:
                            st.session_state.generating_question = True
                            st.session_state.question_ready = False
                            ai_service = get_ai_service()
                            
                            # Get skills and difficulty from session state
                            skills_list = st.session_state.get('extracted_skills', [])
                            skills_str = ", ".join(skills_list) if skills_list else ""
                            
                            difficulty_dist = st.session_state.get('difficulty_distribution', 
                                                                   {"basic": 30, "intermediate": 50, "advanced": 20})
                            
                            # Get difficulty for current question number
                            next_q_num = st.session_state.question_count + 1
                            difficulty = ai_service.get_difficulty_for_question(
                                next_q_num, 
                                st.session_state.total_questions, 
                                difficulty_dist
                            )
                            
                            next_question = ai_service.generate_question(
                                job_role=st.session_state.job_role,
                                skills=skills_str,
                                difficulty=difficulty,
                                previous_questions=st.session_state.previous_questions
                            )
                            st.session_state.current_question = next_question
                            st.session_state.previous_questions.append(next_question)
                            st.session_state.question_count += 1
                            st.session_state.generating_question = False
                            st.session_state.question_ready = True
                        except:
                            st.session_state.generating_question = False
                            st.session_state.question_ready = True
                            fallback_questions = [
                                "Can you describe a challenging problem you faced at work and how you handled it?",
                                "How do you stay updated with the latest trends in your field?", 
                                "Tell me about a project you're particularly proud of.",
                            ]
                            st.session_state.current_question = fallback_questions[(st.session_state.question_count - 1) % len(fallback_questions)]
                            st.session_state.previous_questions.append(st.session_state.current_question)
                            st.session_state.question_count += 1
                
                st.rerun()
        
        with col3:
            # Submit button - changes based on answer mode
            current_answer_mode = st.session_state.question_types[st.session_state.question_count - 1] if st.session_state.question_count <= len(st.session_state.question_types) else 'voice'
            
            if current_answer_mode == 'text':
                # Text submission button
                if st.button("📤 Submit Answer", use_container_width=True, 
                            disabled=not st.session_state.text_answer,
                            type="primary"):
                    
                    transcript = st.session_state.text_answer
                    
                    if transcript.strip():
                        # Evaluate answer
                        with st.spinner("Evaluating your answer..."):
                            ai_service = get_ai_service()
                            evaluation = ai_service.evaluate_answer(
                                question=st.session_state.current_question,
                                answer=transcript
                            )
                            
                            # Store evaluation
                            st.session_state.evaluations.append(evaluation)
                            save_session_snapshot()
                            
                            # Save to database
                            try:
                                db = SessionLocal()
                                interview = LegacyInterview(
                                    candidate_name=st.session_state.candidate_name,
                                    job_role=st.session_state.job_role,
                                    question=st.session_state.current_question,
                                    answer_transcript=transcript,
                                    technical_score=evaluation.get('technical', 0),
                                    clarity_score=evaluation.get('clarity', 0),
                                    communication_score=evaluation.get('communication', 0),
                                    total_score=evaluation.get('total', 0),
                                    feedback=evaluation.get('feedback', ''),
                                    audio_file_path=None
                                )
                                db.add(interview)
                                db.commit()
                                db.close()
                            except Exception as e:
                                pass  # Silently fail database save to not interrupt interview
                            
                            st.success("✅ Answer submitted and evaluated!")
                            
                            # Clear text answer
                            st.session_state.text_answer = ""
                            
                            # Auto-generate next question
                            if st.session_state.question_count < st.session_state.total_questions:
                                with st.spinner("🤖 Generating next question..."):
                                    try:
                                        st.session_state.generating_question = True
                                        st.session_state.question_ready = False
                                        
                                        # Get skills and difficulty from session state
                                        skills_list = st.session_state.get('extracted_skills', [])
                                        skills_str = ", ".join(skills_list) if skills_list else ""
                                        
                                        difficulty_dist = st.session_state.get('difficulty_distribution', 
                                                                               {"basic": 30, "intermediate": 50, "advanced": 20})
                                        
                                        # Get difficulty for next question
                                        next_q_num = st.session_state.question_count + 1
                                        difficulty = ai_service.get_difficulty_for_question(
                                            next_q_num, 
                                            st.session_state.total_questions, 
                                            difficulty_dist
                                        )
                                        
                                        next_question = ai_service.generate_question(
                                            job_role=st.session_state.job_role,
                                            skills=skills_str,
                                            difficulty=difficulty,
                                            previous_questions=st.session_state.previous_questions
                                        )
                                        st.session_state.current_question = next_question
                                        st.session_state.previous_questions.append(next_question)
                                        st.session_state.question_count += 1
                                        st.session_state.generating_question = False
                                        st.session_state.question_ready = True
                                    except:
                                        st.session_state.generating_question = False
                                        st.session_state.question_ready = True
                                        fallback_questions = [
                                            "Can you describe a challenging problem you faced at work and how you handled it?",
                                            "How do you stay updated with the latest trends in your field?",
                                            "Describe a situation where you had to work with a difficult team member.",
                                            "What is your approach to learning new technologies or skills?",
                                            "Tell me about a project you're particularly proud of.",
                                        ]
                                        st.session_state.current_question = fallback_questions[st.session_state.question_count % len(fallback_questions)]
                                        st.session_state.previous_questions.append(st.session_state.current_question)
                                        st.session_state.question_count += 1
                            else:
                                st.success("🎉 All questions completed!")
                            
                            st.rerun()
            else:
                # Voice submit button - uses audio captured by st.audio_input above
                voice_ready = st.session_state.get('voice_answer_ready', False) and st.session_state.get('voice_bytes')
                if st.button("✅ Submit Voice Answer", use_container_width=True,
                            disabled=not voice_ready or not st.session_state.current_question,
                            type="primary"):
                    import tempfile, os as _os
                    voice_bytes = st.session_state.get('voice_bytes')

                    # Reset recording state first
                    st.session_state.voice_answer_ready = False
                    st.session_state.voice_bytes = None

                    # Save raw bytes to temp file — Whisper (ffmpeg) handles WebM/Ogg/WAV
                    tmp_path = None
                    try:
                        with tempfile.NamedTemporaryFile(suffix='.webm', delete=False) as tmp:
                            tmp.write(voice_bytes)
                            tmp_path = tmp.name
                        audio_path = tmp_path
                    except Exception as _e:
                        st.error(f"Failed to save audio: {_e}")
                        audio_path = None

                    if audio_path:
                        # Transcribe with progress indicator
                        with st.spinner("🔄 Transcribing your answer..."):
                            stt_service = get_stt_service()
                            result = stt_service.transcribe_audio(audio_path)
                            # Clean up temp file
                            try:
                                _os.unlink(tmp_path)
                            except Exception:
                                pass
                        
                        if result['success']:
                            transcript = result['transcript']
                            
                            # Evaluate answer
                            with st.spinner("🤔 Evaluating your answer..."):
                                ai_service = get_ai_service()
                                evaluation = ai_service.evaluate_answer(
                                    question=st.session_state.current_question,
                                    answer=transcript
                                )
                            
                            # Store evaluation
                            st.session_state.evaluations.append(evaluation)
                            save_session_snapshot()
                            
                            # Save to database
                            try:
                                db = SessionLocal()
                                interview = LegacyInterview(
                                    candidate_name=st.session_state.candidate_name,
                                    job_role=st.session_state.job_role,
                                    question=st.session_state.current_question,
                                    answer_transcript=transcript,
                                    technical_score=evaluation.get('technical', 0),
                                    clarity_score=evaluation.get('clarity', 0),
                                    communication_score=evaluation.get('communication', 0),
                                    total_score=evaluation.get('total', 0),
                                    feedback=evaluation.get('feedback', ''),
                                    audio_file_path=audio_path
                                )
                                db.add(interview)
                                db.commit()
                                db.close()
                            except Exception as e:
                                pass  # Silently fail database save to not interrupt interview
                            
                            st.success("✅ Answer recorded and evaluated!")
                            
                            # Auto-generate next question if not at limit
                            if st.session_state.question_count < st.session_state.total_questions:
                                with st.spinner("🤖 Generating next question..."):
                                    try:
                                        st.session_state.generating_question = True
                                        st.session_state.question_ready = False
                                        
                                        # Get skills and difficulty from session state
                                        skills_list = st.session_state.get('extracted_skills', [])
                                        skills_str = ", ".join(skills_list) if skills_list else ""
                                        
                                        difficulty_dist = st.session_state.get('difficulty_distribution', 
                                                                               {"basic": 30, "intermediate": 50, "advanced": 20})
                                        
                                        # Get difficulty for next question
                                        next_q_num = st.session_state.question_count + 1
                                        difficulty = ai_service.get_difficulty_for_question(
                                            next_q_num, 
                                            st.session_state.total_questions, 
                                            difficulty_dist
                                        )
                                        
                                        next_question = ai_service.generate_question(
                                            job_role=st.session_state.job_role,
                                            skills=skills_str,
                                            difficulty=difficulty,
                                            previous_questions=st.session_state.previous_questions
                                        )
                                        st.session_state.current_question = next_question
                                        st.session_state.previous_questions.append(next_question)
                                        st.session_state.question_count += 1
                                        st.session_state.generating_question = False
                                        st.session_state.question_ready = True
                                    except:
                                        st.session_state.generating_question = False
                                        st.session_state.question_ready = True
                                        fallback_questions = [
                                            "Can you describe a challenging problem you faced at work and how you handled it?",
                                            "How do you stay updated with the latest trends in your field?",
                                            "Describe a situation where you had to work with a difficult team member.",
                                            "What is your approach to learning new technologies or skills?",
                                            "Tell me about a project you're particularly proud of.",
                                        ]
                                        st.session_state.current_question = fallback_questions[st.session_state.question_count % len(fallback_questions)]
                                        st.session_state.previous_questions.append(st.session_state.current_question)
                                        st.session_state.question_count += 1
                            else:
                                st.success("🎉 All questions completed!")
                            
                            st.rerun()
                        else:
                            st.error(f"Transcription failed: {result.get('error', 'Unknown error')}")
        
        with col4:
            if st.button("📝 Add Note", use_container_width=True):
                note = st.text_input("Add a note:", key=f"note_{len(st.session_state.notes)}")
                if note:
                    st.session_state.notes.append({
                        'time': datetime.now(),
                        'note': note
                    })
                    st.success("Note added!")
        
        with col5:
            if st.button("🛑 End Interview", use_container_width=True, type="secondary"):
                # Mark all questions as complete to show final score
                st.session_state.total_questions = len(st.session_state.evaluations)
                st.success("Ending interview and showing final results...")
                st.rerun()


def main():
    """Main application"""
    if not st.session_state.interview_started:
        setup_page()
    else:
        interview_page()


if __name__ == "__main__":
    main()
