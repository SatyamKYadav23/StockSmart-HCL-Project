import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected system error occurred.';

      if (error.error instanceof ErrorEvent) {
        // Client-side or network error
        errorMessage = `Network error: ${error.error.message}`;
      } else {
        // Backend returned an unsuccessful response code
        switch (error.status) {
          case 400:
            errorMessage = error.error?.message || 'Invalid request parameters.';
            break;
          case 401:
            errorMessage = 'Session expired or unauthorized. Please re-authenticate.';
            break;
          case 403:
            errorMessage = 'Access denied. You do not have permissions for this action.';
            break;
          case 404:
            errorMessage = error.error?.message || 'The requested resource was not found.';
            break;
          case 409:
            errorMessage = error.error?.message || 'Conflict: Record already exists or state invalid.';
            break;
          case 500:
          case 502:
          case 503:
            errorMessage = 'Backend server unavailable. Please try again shortly.';
            break;
          default:
            errorMessage = error.error?.message || `Server error [${error.status}]`;
        }
      }

      console.error(`[StockSmart API Error] ${req.method} ${req.url}:`, errorMessage);
      return throwError(() => new Error(errorMessage));
    })
  );
};
