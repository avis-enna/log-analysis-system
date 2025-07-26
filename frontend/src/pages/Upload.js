import React, { useState } from 'react';
import {
  CloudArrowUpIcon,
  DocumentTextIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline';
import { uploadAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner';

/**
 * Upload page for log files and text
 */
const Upload = () => {
  const [activeTab, setActiveTab] = useState('file');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState(null);
  const [error, setError] = useState(null);

  // File upload state
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [source, setSource] = useState('');

  // Text upload state
  const [logText, setLogText] = useState('');
  const [textSource, setTextSource] = useState('');

  // Handle file selection
  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);
    setSelectedFiles(files);
    setError(null);
    setUploadResult(null);
  };

  // Handle file upload
  const handleFileUpload = async (e) => {
    e.preventDefault();
    
    if (selectedFiles.length === 0) {
      setError('Please select at least one file to upload');
      return;
    }

    setIsUploading(true);
    setError(null);
    
    try {
      const results = [];
      
      for (const file of selectedFiles) {
        const result = await uploadAPI.uploadLogFile(file, source || file.name);
        results.push(result.data);
      }
      
      setUploadResult({
        type: 'file',
        files: selectedFiles.length,
        results: results,
        message: `Successfully uploaded ${selectedFiles.length} file(s)`
      });
      
      // Reset form
      setSelectedFiles([]);
      setSource('');
      if (document.getElementById('file-upload')) {
        document.getElementById('file-upload').value = '';
      }
      
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  // Handle text upload
  const handleTextUpload = async (e) => {
    e.preventDefault();
    
    if (!logText.trim()) {
      setError('Please enter log text to upload');
      return;
    }

    setIsUploading(true);
    setError(null);
    
    try {
      const result = await uploadAPI.uploadLogText(logText, textSource || 'text-upload');
      
      setUploadResult({
        type: 'text',
        message: 'Successfully uploaded log text',
        result: result.data
      });
      
      // Reset form
      setLogText('');
      setTextSource('');
      
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  // Format file size
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="border-b border-gray-200 dark:border-gray-700 pb-4">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Upload Logs
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Upload log files or paste log text for analysis
        </p>
      </div>

      {/* Upload Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('file')}
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'file'
                ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
            }`}
          >
            <CloudArrowUpIcon className="w-5 h-5 inline mr-2" />
            File Upload
          </button>
          <button
            onClick={() => setActiveTab('text')}
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'text'
                ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
            }`}
          >
            <DocumentTextIcon className="w-5 h-5 inline mr-2" />
            Text Upload
          </button>
        </nav>
      </div>

      {/* Results/Error Messages */}
      {error && (
        <div className="rounded-md bg-red-50 dark:bg-red-900/20 p-4">
          <div className="flex">
            <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800 dark:text-red-200">
                Upload Error
              </h3>
              <div className="mt-2 text-sm text-red-700 dark:text-red-300">
                {error}
              </div>
            </div>
          </div>
        </div>
      )}

      {uploadResult && (
        <div className="rounded-md bg-green-50 dark:bg-green-900/20 p-4">
          <div className="flex">
            <CheckCircleIcon className="h-5 w-5 text-green-400" />
            <div className="ml-3">
              <h3 className="text-sm font-medium text-green-800 dark:text-green-200">
                Upload Successful
              </h3>
              <div className="mt-2 text-sm text-green-700 dark:text-green-300">
                {uploadResult.message}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Upload Content */}
      <div className="bg-white dark:bg-gray-800 shadow rounded-lg">
        <div className="px-6 py-6">
          {activeTab === 'file' ? (
            /* File Upload Tab */
            <form onSubmit={handleFileUpload} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Select Log Files
                </label>
                <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 dark:border-gray-600 border-dashed rounded-md hover:border-gray-400 dark:hover:border-gray-500 transition-colors">
                  <div className="space-y-1 text-center">
                    <CloudArrowUpIcon className="mx-auto h-12 w-12 text-gray-400" />
                    <div className="flex text-sm text-gray-600 dark:text-gray-400">
                      <label
                        htmlFor="file-upload"
                        className="relative cursor-pointer rounded-md font-medium text-primary-600 hover:text-primary-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-primary-500"
                      >
                        <span>Upload files</span>
                        <input
                          id="file-upload"
                          name="file-upload"
                          type="file"
                          className="sr-only"
                          multiple
                          accept=".log,.txt,.json"
                          onChange={handleFileChange}
                        />
                      </label>
                      <p className="pl-1">or drag and drop</p>
                    </div>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      .log, .txt, .json files up to 100MB each
                    </p>
                  </div>
                </div>
              </div>

              {/* Selected Files */}
              {selectedFiles.length > 0 && (
                <div>
                  <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Selected Files ({selectedFiles.length})
                  </h4>
                  <div className="space-y-2">
                    {selectedFiles.map((file, index) => (
                      <div key={index} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-md">
                        <div className="flex items-center">
                          <DocumentTextIcon className="h-5 w-5 text-gray-400 mr-2" />
                          <span className="text-sm text-gray-900 dark:text-gray-100">{file.name}</span>
                        </div>
                        <span className="text-sm text-gray-500 dark:text-gray-400">
                          {formatFileSize(file.size)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Source Field */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Source (Optional)
                </label>
                <input
                  type="text"
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  placeholder="e.g., web-server, application-logs"
                  className="block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:text-gray-100"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  If not specified, the filename will be used as the source
                </p>
              </div>

              {/* Upload Button */}
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isUploading || selectedFiles.length === 0}
                  className="btn btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                >
                  {isUploading ? (
                    <LoadingSpinner size="sm" className="mr-2" />
                  ) : (
                    <CloudArrowUpIcon className="w-4 h-4 mr-2" />
                  )}
                  {isUploading ? 'Uploading...' : 'Upload Files'}
                </button>
              </div>
            </form>
          ) : (
            /* Text Upload Tab */
            <form onSubmit={handleTextUpload} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Log Text
                </label>
                <textarea
                  value={logText}
                  onChange={(e) => setLogText(e.target.value)}
                  rows={15}
                  placeholder="Paste your log entries here..."
                  className="block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:text-gray-100 font-mono text-sm"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Each line will be treated as a separate log entry
                </p>
              </div>

              {/* Source Field */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Source (Optional)
                </label>
                <input
                  type="text"
                  value={textSource}
                  onChange={(e) => setTextSource(e.target.value)}
                  placeholder="e.g., web-server, application-logs"
                  className="block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:text-gray-100"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Default: "text-upload"
                </p>
              </div>

              {/* Upload Button */}
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isUploading || !logText.trim()}
                  className="btn btn-primary disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                >
                  {isUploading ? (
                    <LoadingSpinner size="sm" className="mr-2" />
                  ) : (
                    <DocumentTextIcon className="w-4 h-4 mr-2" />
                  )}
                  {isUploading ? 'Uploading...' : 'Upload Text'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default Upload;
