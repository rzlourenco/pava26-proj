(defmacro def-class (class &rest fields)
  (let ((classname (if (atom class) class (first class)))
        (superclasses (if (atom class) nil (rest class))))
  (labels
    ((make-name (&rest names)
       (intern (format nil "~{~a~}" names)))

     (make-keyword (&rest names)
       (intern (format nil "~{~a~}" names) "KEYWORD"))

     (get-fields (class)
       (let ((constructor-name (make-name "MAKE-" class)))
         (apply #'append
                (get constructor-name :fields)
                (mapcar #'get-fields (get constructor-name :super)))))

     (get-superclass-fields (superclasses)
       (mapcar
         (lambda (class) `(,class . ,(get-fields class)))
         superclasses))

     (get-all-fields (superclasses)
       (remove-duplicates
         (apply #'append
           fields
           (mapcar #'cdr
             (get-superclass-fields superclasses)))))

     (call-superclass-constructor (superclass params all-args)
       (let ((constructor-name (make-name "MAKE-" superclass))
             (args (intersection all-args params)))
         `(,constructor-name
            ,@(apply #'append
                     (mapcar
                       (lambda (arg) (list (make-keyword arg) arg))
                       args)))))
     (make-getter (field)
       (let ((getter-name (make-name classname "-" field)))
         `(defun ,getter-name (arg &optional newval)
            (when (or (not arg) (not (symbolp arg)))
              (return-from ,getter-name))
            (labels
              ((find-field (obj can-return)
                (let* ((fields (get obj :fields))
                       (field (assoc ',field fields)))
                  (cond
                    ((not obj) nil)
                    ((and field can-return) field)
                    (t (car (mapcar
                      (lambda (s)
                        (find-field
                          s
                          (or can-return (eq (get s :type) ',classname))))
                      (get obj :super))))))))
              (let ((field (find-field arg (eq (get arg :type) ',classname))))
              (if newval
                (setf (cdr field) newval)
                (cdr field)))))))
     (make-setter (field)
       (let ((setter-name (make-name classname "-" field)))
         `(defsetf ,setter-name ,setter-name))))
    (let* ((superclass-field-pairs (get-superclass-fields superclasses))
           (all-fields
             (remove-duplicates
               (apply #'append
                 fields
                 (mapcar #'cdr superclass-field-pairs))))
           (constructor-name (make-name "MAKE-" classname))
           (predicate-name (make-name classname "?")))
      `(progn
         ,@(mapcar #'make-getter all-fields)
         ,@(mapcar #'make-setter all-fields)

         (defun ,predicate-name (arg)
           (cond ((not arg) nil)
                 ((not (symbolp arg) ) nil)
                 ((eq (get arg :type) ',classname) t)
                 (t (some #'identity (mapcar #',predicate-name (get arg :super))))))

         (defun ,constructor-name (&key ,@all-fields)
           (let ((sym (gensym))
                 (super
                   ,(when
                      superclasses
                      `(list ,@(mapcar
                                 (lambda (pair)
                                   (call-superclass-constructor
                                     (car pair)
                                     (cdr pair)
                                     all-fields))
                                 superclass-field-pairs)))))
             (setf (get sym :super) super)
             (setf (get sym :type) ',classname)
             (setf (get sym :fields)
                   (list ,@(mapcar
                             (lambda (e) `(cons ',e ,e))
                             fields)))
             sym))

         (setf (get ',constructor-name :args) ',all-fields)
         (setf (get ',constructor-name :fields) ',fields)
         (setf (get ',constructor-name :super) ',superclasses)
         nil)))))

;(pprint
;  (macroexpand `(def-class person name age)))
(def-class person name age)
;(pprint
;  (macroexpand `(def-class (student person) course)))
(def-class (student person) course)
;(pprint
;  (macroexpand `(def-class sportsman activity schedule)))
(def-class sportsman activity schedule)
;(pprint
;  (macroexpand `(def-class (ist-student student sportsman))))
(def-class (ist-student student sportsman))

(format t "~%")
(let ((a (make-person :name "Paulo" :age 33))
      (b "I am not a person"))
  (pprint (list (person? a) (person? b))))

(let ((s (make-student :name "Paul" :age 21 :course "Informatics")))
  (pprint (list (student-name s) (student-course s))))

(let ((p (make-person :name "John" :age 34))
      (s (make-student :name "Paul" :age 21 :course "Informatics")))
  (pprint (list (person? p) (student? p) (person? s) (student? s))))

(let ((m (make-ist-student :name "Maria" :course "IA" :activity "Tennis")))
  (pprint (list (ist-student? m) (student? m) (sportsman? m) (ist-student-name m) (person-name m) (sportsman-activity m) (ist-student-activity m))))
