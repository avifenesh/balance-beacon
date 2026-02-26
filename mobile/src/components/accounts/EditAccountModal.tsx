import React, { useState, useEffect } from 'react'
import {
  Modal,
  View,
  Text,
  StyleSheet,
  Pressable,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native'
import { useAccountsStore, type Account, type DbAccountType } from '../../stores/accountsStore'
import { useToastStore } from '../../stores/toastStore'
import { validateAccountName } from '../../utils/validation'
import { AccountForm, type AccountFormValues } from './AccountForm'

interface EditAccountModalProps {
  visible: boolean
  onClose: () => void
  account: Account | null
}

const INITIAL_VALUES: AccountFormValues = {
  name: '',
  type: 'SELF' as DbAccountType,
  color: null,
  preferredCurrency: null,
}

export function EditAccountModal({ visible, onClose, account }: EditAccountModalProps) {
  const [values, setValues] = useState<AccountFormValues>(INITIAL_VALUES)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (visible && account) {
      setValues({
        name: account.name,
        type: account.dbType,
        color: account.color,
        preferredCurrency: account.preferredCurrency,
      })
      setError(null)
      setIsSubmitting(false)
    }
  }, [visible, account])

  const handleChange = (key: keyof AccountFormValues, value: any) => {
    setValues((prev) => ({ ...prev, [key]: value }))
    if (key === 'name') setError(null)
  }

  const handleSave = async () => {
    if (!account) return

    const validation = validateAccountName(values.name)
    if (!validation.valid) {
      setError(validation.error || 'Invalid name')
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      const success = await useAccountsStore.getState().updateAccount(account.id, {
        name: values.name.trim(),
        type: values.type,
        color: values.color,
        preferredCurrency: values.preferredCurrency,
      })

      if (success) {
        useToastStore.getState().success('Account updated')
        onClose()
      } else {
        const storeError = useAccountsStore.getState().error
        setError(storeError || 'Failed to update account')
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update account'
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!account) return null

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <KeyboardAvoidingView
        style={styles.modalOverlay}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.modalContent} testID="accounts.editModal">
          <Text style={styles.modalTitle}>Edit Account</Text>

          <ScrollView style={styles.modalScrollContent} showsVerticalScrollIndicator={false}>
            <AccountForm
              values={values}
              onChange={handleChange}
              errors={error ? { name: error } : {}}
              isSubmitting={isSubmitting}
              testID="accounts.editModal"
            />
          </ScrollView>

          <View style={styles.modalActions}>
            <Pressable
              style={styles.modalCancelButton}
              onPress={onClose}
              disabled={isSubmitting}
              testID="accounts.editModal.cancelButton"
            >
              <Text style={styles.modalCancelText}>Cancel</Text>
            </Pressable>
            <Pressable
              style={[
                styles.modalSaveButton,
                isSubmitting && styles.modalSaveButtonDisabled,
              ]}
              onPress={handleSave}
              disabled={isSubmitting}
              testID="accounts.editModal.saveButton"
            >
              {isSubmitting ? (
                <ActivityIndicator size="small" color="#0f172a" />
              ) : (
                <Text style={styles.modalSaveText}>Save</Text>
              )}
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  )
}

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalBackdrop: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.7)',
  },
  modalContent: {
    backgroundColor: '#1e293b',
    borderRadius: 16,
    padding: 24,
    width: '85%',
    maxWidth: 360,
    maxHeight: '80%',
  },
  modalScrollContent: {
    flexGrow: 0,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#fff',
    textAlign: 'center',
    marginBottom: 20,
  },
  modalActions: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 8,
  },
  modalCancelButton: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  modalCancelText: {
    fontSize: 16,
    color: '#94a3b8',
  },
  modalSaveButton: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: '#38bdf8',
  },
  modalSaveButtonDisabled: {
    opacity: 0.6,
  },
  modalSaveText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#0f172a',
  },
})
