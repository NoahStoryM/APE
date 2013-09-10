;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Writing Hygienic Macros in Scheme with Syntax-case
;; R. Kent Dybvig
;; Technical Report #356
;;
;; page 7
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#lang racket

(require "print-test.rkt")

(define-syntax (when* x)
  (syntax-case x ()
    [(_ e0 e1 e2 ...)
     #'(if e0 (begin e1 e2 ...) (void))]))

(define-syntax (unless* x)
  (syntax-case x ()
    [(_ e0 e1 e2 ...)
     #'(when* (not e0) e1 e2 ...)]))

(print-test (when* (positive? -5)
              (display "hi"))

            (when* (positive? 5)
              (display "hi")
              (displayln " there"))

            (unless* (positive? 5)
              (display "hi"))

            (unless* (positive? -5)
              (display "hi")
              (displayln " there"))

            ; In hygienic macro, the new binding for `begin` and `if`
            ; introduce by the `let` will not alter the semantics of the
            ; when*.
            (let ([begin list]
                  [if 'lala])
              (when* #t (write "win") (newline))))