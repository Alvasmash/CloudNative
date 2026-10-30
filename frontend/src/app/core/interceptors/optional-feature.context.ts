import { HttpContextToken } from '@angular/common/http';

// A 404 for an optional server feature is handled explicitly by its component.
// Authentication and permission failures must still pass through normal error handling.
export const OPTIONAL_SERVER_FEATURE = new HttpContextToken<boolean>(() => false);
