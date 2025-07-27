import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { ExclamationTriangleIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { closeModal } from '../../store/slices/uiSlice';

/**
 * Confirm Dialog Modal Component
 */
const ConfirmDialog = () => {
  const dispatch = useDispatch();
  const modal = useSelector(state => state.ui.modals.confirmDialog);

  if (!modal.open) return null;

  const handleConfirm = () => {
    if (modal.onConfirm) {
      modal.onConfirm();
    }
    dispatch(closeModal('confirmDialog'));
  };

  const handleCancel = () => {
    if (modal.onCancel) {
      modal.onCancel();
    }
    dispatch(closeModal('confirmDialog'));
  };

  const getIconColor = () => {
    switch (modal.type) {
      case 'warning':
        return 'text-yellow-600';
      case 'error':
        return 'text-red-600';
      case 'success':
        return 'text-green-600';
      default:
        return 'text-blue-600';
    }
  };

  const getButtonColor = () => {
    switch (modal.type) {
      case 'warning':
        return 'bg-yellow-600 hover:bg-yellow-700 focus:ring-yellow-500';
      case 'error':
        return 'bg-red-600 hover:bg-red-700 focus:ring-red-500';
      case 'success':
        return 'bg-green-600 hover:bg-green-700 focus:ring-green-500';
      default:
        return 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500';
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
        {/* Backdrop */}
        <div 
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={handleCancel}
        />
        
        {/* Modal Panel */}
        <div className="relative transform overflow-hidden rounded-lg bg-white dark:bg-gray-800 px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
          <div className="sm:flex sm:items-start">
            <div className={`mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full sm:mx-0 sm:h-10 sm:w-10 ${
              modal.type === 'warning' ? 'bg-yellow-100 dark:bg-yellow-900' :
              modal.type === 'error' ? 'bg-red-100 dark:bg-red-900' :
              modal.type === 'success' ? 'bg-green-100 dark:bg-green-900' :
              'bg-blue-100 dark:bg-blue-900'
            }`}>
              <ExclamationTriangleIcon className={`h-6 w-6 ${getIconColor()}`} aria-hidden="true" />
            </div>
            <div className="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left">
              <h3 className="text-base font-semibold leading-6 text-gray-900 dark:text-white">
                {modal.title}
              </h3>
              <div className="mt-2">
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {modal.message}
                </p>
              </div>
            </div>
          </div>
          <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
            <button
              type="button"
              className={`inline-flex w-full justify-center rounded-md px-3 py-2 text-sm font-semibold text-white shadow-sm sm:ml-3 sm:w-auto ${getButtonColor()}`}
              onClick={handleConfirm}
            >
              {modal.confirmText}
            </button>
            <button
              type="button"
              className="mt-3 inline-flex w-full justify-center rounded-md bg-white dark:bg-gray-700 px-3 py-2 text-sm font-semibold text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-600 sm:mt-0 sm:w-auto"
              onClick={handleCancel}
            >
              {modal.cancelText}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDialog;
