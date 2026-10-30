import { HttpContextToken } from '@angular/common/http';

/** Forms display their own accessible, contextual errors. */
export const FORM_HANDLES_ERRORS = new HttpContextToken<boolean>(() => false);
