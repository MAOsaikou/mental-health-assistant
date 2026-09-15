import DOMPurify from 'dompurify'

const SANITIZE_CONFIG = Object.freeze({
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's',
    'h1', 'h2', 'h3', 'h4', 'blockquote',
    'ul', 'ol', 'li', 'pre', 'code', 'a', 'span', 'div',
  ],
  ALLOWED_ATTR: ['href', 'title', 'rel', 'class'],
  ALLOW_DATA_ATTR: false,
  ALLOW_ARIA_ATTR: false,
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[/?#.]|[^:]+$)/i,
})

export function sanitizeHtml(value) {
  return DOMPurify.sanitize(String(value ?? ''), SANITIZE_CONFIG)
}
