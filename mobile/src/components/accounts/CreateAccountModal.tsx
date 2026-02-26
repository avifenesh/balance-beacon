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
import { useAccountsStore, type DbAccountType } from '../../stores/accountsStore'
import { useToastStore } from '../../stores/toastStore'
import { validateAccountName } from '../../utils/validation'
import { AccountForm, type AccountFormValues } from './AccountForm'

interface CreateAccountModalProps {
  visible: boolean
  onClose: () => void
}

const INITIAL_VALUES: AccountFormValues = {
  name: '',
  type: 'SELF' as DbAccountType,
  color: '#22c55e',
  preferredCurrency: null,
}

export function CreateAccountModal({ visible, onClose }: CreateAccountModalProps) {
  const [values, setValues] = useState<AccountFormValues>(INITIAL_VALUES)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Reset values when modal opens
  useEffect(() => {
    if (visible) {
      setValues(INITIAL_VALUES)
      setError(null)
      setIsSubmitting(false)
    }
  }, [visible])

  const handleChange = (key: keyof AccountFormValues, value: any) => {
    setValues((prev) => ({ ...prev, [key]: value }))
    if (key === 'name') setError(null)
  }

  const handleCreate = async () => {
    const validation = validateAccountName(values.name)
    if (!validation.valid) {
      setError(validation.error || 'Invalid name')
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      const success = await useAccountsStore.getState().createAccount({
        name: values.name.trim(),
        type: values.type,
        color: values.color,
        preferredCurrency: values.preferredCurrency,
      })

      if (success) {
        useToastStore.getState().success('Account created')
        onClose()
      } else {
        const storeError = useAccountsStore.getState().error
        setError(storeError || 'Failed to create account')
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create account'
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

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
        <View style={styles.modalContent} testID="accounts.createModal">
          <Text style={styles.modalTitle}>Create Account</Text>

          <ScrollView style={styles.modalScrollContent} showsVerticalScrollIndicator={false}>
            <AccountForm
              values={values}
              onChange={handleChange}
              errors={error ? { name: error } : {}}
              isSubmitting={isSubmitting}
              testID="accounts.createModal"
            />
          </ScrollView>

          <View style={styles.modalActions}>
            <Pressable
              style={styles.modalCancelButton}
              onPress={onClose}
              disabled={isSubmitting}
              testID="accounts.createModal.cancelButton"
            >
              <Text style={styles.modalCancelText}>Cancel</Text>
            </Pressable>
            <Pressable
              style={[
                styles.modalSaveButton,
                isSubmitting && styles.modalSaveButtonDisabled,
              ]}
              onPress={handleCreate}
              disabled={isSubmitting}
              testID="accounts.createModal.saveButton"
            >
              {isSubmitting ? (
                <ActivityIndicator size="small" color="#0f172a" />
              ) : (
                <Text style={styles.modalSaveText}>Create</Text>
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
