import { useRef, useState, type DragEvent } from 'react'

type Props = {
  title: string
  formats: string
  accept: string
  file: File | null
  text: string
  disabled?: boolean
  onFile: (file: File | null) => void
  onText: (value: string) => void
}

export default function FilePanel({
  title,
  formats,
  accept,
  file,
  text,
  disabled,
  onFile,
  onText,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [active, setActive] = useState(false)

  function onDrop(event: DragEvent) {
    event.preventDefault()
    setActive(false)
    const next = event.dataTransfer.files[0]
    if (next && !disabled) {
      onFile(next)
    }
  }

  return (
    <div
      className={`drop ${active ? 'active' : ''}`}
      onDragOver={(event) => {
        event.preventDefault()
        setActive(true)
      }}
      onDragLeave={() => setActive(false)}
      onDrop={onDrop}
    >
      <header>
        <h2>{title}</h2>
        <small>{formats}</small>
      </header>
      <button
        className="file-btn"
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
      >
        {file ? 'Replace file' : 'Drop a file or browse'}
      </button>
      {file ? (
        <div className="file-row">
          <span className="file-name">{file.name}</span>
          <button className="ghost" type="button" disabled={disabled} onClick={() => onFile(null)}>
            Remove
          </button>
        </div>
      ) : null}
      <textarea
        aria-label={`${title} text`}
        placeholder={file ? 'The file above is used instead of pasted text' : 'Or paste text here'}
        value={text}
        disabled={disabled || Boolean(file)}
        onChange={(event) => onText(event.target.value)}
      />
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        hidden
        onChange={(event) => {
          onFile(event.target.files?.[0] ?? null)
          event.target.value = ''
        }}
      />
    </div>
  )
}
